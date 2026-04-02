package edusecure.edusecure.service.space;

import edusecure.edusecure.audit.AuditService;
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

    @Transactional(readOnly = true)
    public List<SpaceSummaryResponse> listSpaces(String currentUserEmail) {
        User currentUser = findUserByEmail(currentUserEmail);

        if (hasRole(currentUser, RoleName.ADMIN)) {
            return mapSummaries(spaceRepository.findAllSummaries(), true, false);
        }

        if (hasRole(currentUser, RoleName.LECTURER)) {
            return mapSummaries(spaceRepository.findSummariesByCreatedByUserId(currentUser.getId()), true, false);
        }

        if (hasRole(currentUser, RoleName.STUDENT)) {
            return mapSummaries(spaceRepository.findSummariesByStudentUserId(currentUser.getId()), false, true);
        }

        return List.of();
    }

    @Transactional
    public SpaceDetailResponse createSpace(String currentUserEmail, CreateSpaceRequest request) {
        User currentUser = findUserByEmail(currentUserEmail);
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
        User currentUser = findUserByEmail(currentUserEmail);
        Space space = getSpaceOrThrow(spaceId);
        boolean canManage = canManageSpace(currentUser, space);
        boolean isMember = spaceMembershipRepository.existsBySpaceIdAndStudentUserId(spaceId, currentUser.getId());

        if (!canManage && !isMember) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to view this space");
        }

        List<SpaceStudentResponse> memberships = canManage ? buildMembershipResponses(spaceId) : List.of();
        return toDetailResponse(space, memberships, canManage, isMember, spaceMembershipRepository.countBySpaceId(spaceId));
    }

    @Transactional
    public SpaceDetailResponse updateSpace(String currentUserEmail, UUID spaceId, UpdateSpaceRequest request) {
        User currentUser = findUserByEmail(currentUserEmail);
        Space space = getSpaceOrThrow(spaceId);
        requireManagePermission(currentUser, space);

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
        User currentUser = findUserByEmail(currentUserEmail);
        Space space = getSpaceOrThrow(spaceId);
        requireManagePermission(currentUser, space);

        if (space.isArchived()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Archived spaces cannot accept new students");
        }

        User student = findUserByEmail(request.studentEmail().trim());
        if (!hasRole(student, RoleName.STUDENT)) {
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

        return toStudentResponse(membership, student);
    }

    @Transactional
    public void removeStudentFromSpace(String currentUserEmail, UUID spaceId, UUID studentUserId) {
        User currentUser = findUserByEmail(currentUserEmail);
        Space space = getSpaceOrThrow(spaceId);
        requireManagePermission(currentUser, space);

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
                space.getCreatedByUserId(),
                space.getCreatedAt(),
                space.getUpdatedAt(),
                memberships
        );
    }

    private void requireManagePermission(User currentUser, Space space) {
        if (!canManageSpace(currentUser, space)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to manage this space");
        }
    }

    private boolean canManageSpace(User currentUser, Space space) {
        return hasRole(currentUser, RoleName.ADMIN)
                || hasRole(currentUser, RoleName.LECTURER) && space.getCreatedByUserId().equals(currentUser.getId());
    }

    private boolean hasRole(User user, RoleName roleName) {
        return user.getRoles().stream().anyMatch(role -> role.getName() == roleName);
    }

    private Space getSpaceOrThrow(UUID spaceId) {
        return spaceRepository.findById(spaceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Space not found"));
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private String normalizeCode(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }
}