package edusecure.edusecure.service.space;

import edusecure.edusecure.audit.AuditService;
import edusecure.edusecure.config.chat.SpaceChatProperties;
import edusecure.edusecure.dto.space.AddSpaceStudentRequest;
import edusecure.edusecure.dto.space.CreateSpaceRequest;
import edusecure.edusecure.dto.space.SpaceDetailResponse;
import edusecure.edusecure.dto.space.SpaceStudentResponse;
import edusecure.edusecure.dto.space.SpaceSummaryResponse;
import edusecure.edusecure.dto.space.UpdateSpaceRequest;
import edusecure.edusecure.entity.audit.AuditActionType;
import edusecure.edusecure.entity.auth.RoleName;
import edusecure.edusecure.entity.auth.User;
import edusecure.edusecure.entity.space.Space;
import edusecure.edusecure.entity.space.SpaceMembership;
import edusecure.edusecure.repository.auth.UserRepository;
import edusecure.edusecure.repository.space.SpaceMembershipRepository;
import edusecure.edusecure.repository.space.SpaceRepository;
import edusecure.edusecure.repository.space.SpaceSummaryProjection;
import edusecure.edusecure.service.spacechatkey.SpaceChatKeyVersionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SpaceService {

    private final SpaceRepository spaceRepository;
    private final SpaceMembershipRepository spaceMembershipRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final SpaceAccessService spaceAccessService;
    private final SpaceChatProperties spaceChatProperties;
    private final SpaceChatKeyVersionService spaceChatKeyVersionService;

    @Transactional(readOnly = true)
    public List<SpaceSummaryResponse> listSpaces(String currentUserEmail) {
        User currentUser = spaceAccessService.findCurrentUserOrThrow(currentUserEmail);

        if (spaceAccessService.hasRole(currentUser, RoleName.ADMIN)) {
            return mapSummaries(spaceRepository.findAllSummaries(), true, false);
        }

        if (spaceAccessService.hasRole(currentUser, RoleName.LECTURER)) {
            return mapSummaries(spaceRepository.findSummariesByCreatedByUserId(currentUser.getId()), true, false);
        }

        if (spaceAccessService.hasRole(currentUser, RoleName.STUDENT)) {
            return mapSummaries(spaceRepository.findSummariesByStudentUserId(currentUser.getId()), false, true);
        }

        return List.of();
    }

    @Transactional
    public SpaceDetailResponse createSpace(String currentUserEmail, CreateSpaceRequest request) {
        User currentUser = spaceAccessService.findCurrentUserOrThrow(currentUserEmail);
        String normalizedCode = normalizeCode(request.code());

        if (spaceRepository.existsByCode(normalizedCode)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Space code already exists");
        }

        Instant now = Instant.now();
        Space saved = spaceRepository.save(Space.builder()
                .name(request.name().trim())
                .code(normalizedCode)
                .description(request.description().trim())
                .createdByUserId(currentUser.getId())
                .createdAt(now)
                .updatedAt(now)
                .archived(false)
                .build());

        auditService.record(
                AuditActionType.SPACE_CREATED,
                currentUser.getId(),
                Space.class.getSimpleName(),
                saved.getId(),
                "code=" + saved.getCode()
        );

        return toDetailResponse(saved, List.of(), true, false, 0);
    }

    @Transactional(readOnly = true)
    public SpaceDetailResponse getSpace(String currentUserEmail, UUID spaceId) {
        SpaceAccessContext accessContext = spaceAccessService.requireReadableSpace(currentUserEmail, spaceId);
        List<SpaceStudentResponse> memberships = accessContext.canManage() ? buildMembershipResponses(spaceId) : List.of();
        return toDetailResponse(
                accessContext.space(),
                memberships,
                accessContext.canManage(),
                accessContext.isMember(),
                spaceMembershipRepository.countBySpaceId(spaceId)
        );
    }

    @Transactional
    public SpaceDetailResponse updateSpace(String currentUserEmail, UUID spaceId, UpdateSpaceRequest request) {
        SpaceAccessContext accessContext = spaceAccessService.requireManageableSpace(currentUserEmail, spaceId);
        User currentUser = accessContext.currentUser();
        Space space = accessContext.space();

        String normalizedCode = normalizeCode(request.code());
        if (spaceRepository.existsByCodeAndIdNot(normalizedCode, space.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Space code already exists");
        }

        space.setName(request.name().trim());
        space.setCode(normalizedCode);
        space.setDescription(request.description().trim());
        space.setArchived(request.archived());
        space.setUpdatedAt(Instant.now());

        Space saved = spaceRepository.save(space);
        auditService.record(
                AuditActionType.SPACE_UPDATED,
                currentUser.getId(),
                Space.class.getSimpleName(),
                saved.getId(),
                "code=" + saved.getCode() + ",archived=" + saved.isArchived()
        );

        List<SpaceStudentResponse> memberships = buildMembershipResponses(saved.getId());
        return toDetailResponse(saved, memberships, true, false, memberships.size());
    }

    @Transactional
    public SpaceStudentResponse addStudentToSpace(String currentUserEmail, UUID spaceId, AddSpaceStudentRequest request) {
        SpaceAccessContext accessContext = spaceAccessService.requireManageableSpace(currentUserEmail, spaceId);
        User currentUser = accessContext.currentUser();
        Space space = accessContext.space();

        if (space.isArchived()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Archived spaces cannot accept new students");
        }

        User student = findUserByEmail(request.studentEmail().trim());
        if (!spaceAccessService.hasRole(student, RoleName.STUDENT)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "User is not a student and cannot be added to a space");
        }

        if (spaceMembershipRepository.existsBySpaceIdAndStudentUserId(spaceId, student.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Student is already assigned to this space");
        }

        SpaceMembership membership = spaceMembershipRepository.save(SpaceMembership.builder()
                .spaceId(spaceId)
                .studentUserId(student.getId())
                .addedByUserId(currentUser.getId())
                .addedAt(Instant.now())
                .build());

        auditService.record(
                AuditActionType.SPACE_STUDENT_ADDED,
                currentUser.getId(),
                Space.class.getSimpleName(),
                space.getId(),
                "spaceCode=" + space.getCode() + ",studentUserId=" + student.getId()
        );
        spaceChatKeyVersionService.markSpaceRequiresRekey(spaceId);

        return toStudentResponse(membership, student);
    }

    @Transactional
    public void removeStudentFromSpace(String currentUserEmail, UUID spaceId, UUID studentUserId) {
        SpaceAccessContext accessContext = spaceAccessService.requireManageableSpace(currentUserEmail, spaceId);
        User currentUser = accessContext.currentUser();
        Space space = accessContext.space();

        SpaceMembership membership = spaceMembershipRepository.findBySpaceIdAndStudentUserId(spaceId, studentUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student is not assigned to this space"));

        spaceMembershipRepository.delete(membership);
        auditService.record(
                AuditActionType.SPACE_STUDENT_REMOVED,
                currentUser.getId(),
                Space.class.getSimpleName(),
                space.getId(),
                "spaceCode=" + space.getCode() + ",studentUserId=" + studentUserId
        );
        spaceChatKeyVersionService.markSpaceRequiresRekey(spaceId);
    }

    private List<SpaceSummaryResponse> mapSummaries(List<SpaceSummaryProjection> projections, boolean canManage, boolean isMember) {
        return projections.stream()
                .map(space -> new SpaceSummaryResponse(
                        space.getId(),
                        space.getName(),
                        space.getCode(),
                        space.getDescription(),
                        space.isArchived(),
                        space.getMemberCount(),
                        canManage,
                        isMember
                ))
                .toList();
    }

    private List<SpaceStudentResponse> buildMembershipResponses(UUID spaceId) {
        List<SpaceMembership> memberships = spaceMembershipRepository.findAllBySpaceIdOrderByAddedAtAsc(spaceId);
        Map<UUID, User> usersById = userRepository.findAllById(
                        memberships.stream().map(SpaceMembership::getStudentUserId).toList()
                ).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        return memberships.stream()
                .map(membership -> {
                    User student = usersById.get(membership.getStudentUserId());
                    if (student == null) {
                        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found for membership");
                    }
                    return toStudentResponse(membership, student);
                })
                .toList();
    }

    private SpaceStudentResponse toStudentResponse(SpaceMembership membership, User student) {
        return new SpaceStudentResponse(
                student.getId(),
                student.getEmail(),
                student.getFullName(),
                membership.getAddedByUserId(),
                membership.getAddedAt()
        );
    }

    private SpaceDetailResponse toDetailResponse(
            Space space,
            List<SpaceStudentResponse> memberships,
            boolean canManage,
            boolean isMember,
            long memberCount
    ) {
        return new SpaceDetailResponse(
                space.getId(),
                space.getName(),
                space.getCode(),
                space.getDescription(),
                space.isArchived(),
                memberCount,
                canManage,
                isMember,
                spaceChatProperties.isEnabled(),
                space.getCreatedByUserId(),
                space.getCreatedAt(),
                space.getUpdatedAt(),
                memberships
        );
    }


    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private String normalizeCode(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }
}