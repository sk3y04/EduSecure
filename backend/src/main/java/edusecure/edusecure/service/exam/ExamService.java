package edusecure.edusecure.service.exam;

import edusecure.edusecure.audit.AuditService;
import edusecure.edusecure.dto.exam.CreateExamRequest;
import edusecure.edusecure.dto.exam.ExamResponse;
import edusecure.edusecure.dto.exam.UpdateExamRequest;
import edusecure.edusecure.entity.audit.AuditActionType;
import edusecure.edusecure.entity.auth.RoleName;
import edusecure.edusecure.entity.auth.User;
import edusecure.edusecure.entity.exam.Exam;
import edusecure.edusecure.entity.space.Space;
import edusecure.edusecure.repository.auth.UserRepository;
import edusecure.edusecure.repository.exam.ExamRepository;
import edusecure.edusecure.repository.space.SpaceMembershipRepository;
import edusecure.edusecure.repository.space.SpaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExamService {

    private final ExamRepository examRepository;
    private final SpaceRepository spaceRepository;
    private final SpaceMembershipRepository spaceMembershipRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    @Transactional
    public ExamResponse createExam(String currentUserEmail, CreateExamRequest request) {
        User currentUser = findUserByEmail(currentUserEmail);
        Space targetSpace = getSpaceOrThrow(request.spaceId());
        requireStaffManagePermission(currentUser, targetSpace);
        requireSpaceWritable(targetSpace);
        validateScheduleWindow(request.startsAt(), request.endsAt());
        ensureNoOverlap(targetSpace.getId(), request.startsAt(), request.endsAt(), null);

        Instant now = Instant.now();
        Exam saved = examRepository.save(Exam.builder()
                .spaceId(targetSpace.getId())
                .title(request.title().trim())
                .description(normalizeOptionalText(request.description()))
                .location(request.location().trim())
                .startsAt(request.startsAt())
                .endsAt(request.endsAt())
                .published(request.published())
                .createdByUserId(currentUser.getId())
                .createdAt(now)
                .updatedAt(now)
                .build());

        auditService.record(
                AuditActionType.EXAM_CREATED,
                currentUser.getId(),
                Exam.class.getSimpleName(),
                saved.getId(),
                buildAuditDetail(targetSpace, saved)
        );

        return toResponse(saved, targetSpace, true);
    }

    @Transactional(readOnly = true)
    public List<ExamResponse> listExams(String currentUserEmail) {
        User currentUser = findUserByEmail(currentUserEmail);

        if (hasRole(currentUser, RoleName.ADMIN)) {
            List<Exam> exams = examRepository.findAllByOrderByStartsAtAsc();
            return mapResponses(exams, true);
        }

        if (hasRole(currentUser, RoleName.LECTURER)) {
            List<UUID> ownedSpaceIds = spaceRepository.findAllByCreatedByUserId(currentUser.getId()).stream()
                    .map(Space::getId)
                    .toList();
            if (ownedSpaceIds.isEmpty()) {
                return List.of();
            }

            return mapResponses(examRepository.findAllBySpaceIdInOrderByStartsAtAsc(ownedSpaceIds), true);
        }

        if (hasRole(currentUser, RoleName.STUDENT)) {
            List<UUID> spaceIds = spaceMembershipRepository.findAllByStudentUserId(currentUser.getId()).stream()
                    .map(membership -> membership.getSpaceId())
                    .distinct()
                    .toList();
            if (spaceIds.isEmpty()) {
                return List.of();
            }

            return mapResponses(examRepository.findAllBySpaceIdInAndPublishedTrueOrderByStartsAtAsc(spaceIds), false);
        }

        return List.of();
    }

    @Transactional(readOnly = true)
    public ExamResponse getExam(String currentUserEmail, UUID examId) {
        User currentUser = findUserByEmail(currentUserEmail);
        Exam exam = getExamOrThrow(examId);
        Space space = getSpaceOrThrow(exam.getSpaceId());

        if (canManageSpace(currentUser, space)) {
            return toResponse(exam, space, true);
        }

        if (hasRole(currentUser, RoleName.STUDENT)
                && exam.isPublished()
                && spaceMembershipRepository.existsBySpaceIdAndStudentUserId(space.getId(), currentUser.getId())) {
            return toResponse(exam, space, false);
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to view this exam");
    }

    @Transactional
    public ExamResponse updateExam(String currentUserEmail, UUID examId, UpdateExamRequest request) {
        User currentUser = findUserByEmail(currentUserEmail);
        Exam exam = getExamOrThrow(examId);
        Space currentSpace = getSpaceOrThrow(exam.getSpaceId());
        requireStaffManagePermission(currentUser, currentSpace);

        Space targetSpace = getSpaceOrThrow(request.spaceId());
        requireStaffManagePermission(currentUser, targetSpace);
        requireSpaceWritable(targetSpace);
        validateScheduleWindow(request.startsAt(), request.endsAt());
        ensureNoOverlap(targetSpace.getId(), request.startsAt(), request.endsAt(), exam.getId());

        exam.setSpaceId(targetSpace.getId());
        exam.setTitle(request.title().trim());
        exam.setDescription(normalizeOptionalText(request.description()));
        exam.setLocation(request.location().trim());
        exam.setStartsAt(request.startsAt());
        exam.setEndsAt(request.endsAt());
        exam.setPublished(request.published());
        exam.setUpdatedAt(Instant.now());

        Exam saved = examRepository.save(exam);
        auditService.record(
                AuditActionType.EXAM_UPDATED,
                currentUser.getId(),
                Exam.class.getSimpleName(),
                saved.getId(),
                buildAuditDetail(targetSpace, saved)
        );

        return toResponse(saved, targetSpace, true);
    }

    private List<ExamResponse> mapResponses(List<Exam> exams, boolean canManage) {
        Map<UUID, Space> spacesById = loadSpacesById(exams);
        return exams.stream()
                .map(exam -> {
                    Space space = spacesById.get(exam.getSpaceId());
                    if (space == null) {
                        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Space not found for exam");
                    }
                    return toResponse(exam, space, canManage);
                })
                .toList();
    }

    private Map<UUID, Space> loadSpacesById(List<Exam> exams) {
        Map<UUID, Space> spacesById = new HashMap<>();
        spaceRepository.findAllById(exams.stream().map(Exam::getSpaceId).distinct().toList())
                .forEach(space -> spacesById.put(space.getId(), space));
        return spacesById;
    }

    private ExamResponse toResponse(Exam exam, Space space, boolean canManage) {
        return new ExamResponse(
                exam.getId(),
                exam.getSpaceId(),
                space.getCode(),
                space.getName(),
                exam.getTitle(),
                exam.getDescription(),
                exam.getLocation(),
                exam.getStartsAt(),
                exam.getEndsAt(),
                exam.isPublished(),
                exam.getCreatedByUserId(),
                exam.getCreatedAt(),
                exam.getUpdatedAt(),
                canManage
        );
    }

    private String buildAuditDetail(Space space, Exam exam) {
        return "spaceCode=" + space.getCode()
                + ",published=" + exam.isPublished()
                + ",startsAt=" + exam.getStartsAt()
                + ",endsAt=" + exam.getEndsAt();
    }

    private void ensureNoOverlap(UUID spaceId, Instant startsAt, Instant endsAt, UUID excludedExamId) {
        if (examRepository.countOverlappingExams(spaceId, startsAt, endsAt, excludedExamId) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Exam schedule overlaps with another exam in this space");
        }
    }

    private void validateScheduleWindow(Instant startsAt, Instant endsAt) {
        if (!endsAt.isAfter(startsAt)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End time must be after start time");
        }
    }

    private void requireSpaceWritable(Space space) {
        if (space.isArchived()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Archived spaces cannot accept exam schedule changes");
        }
    }

    private void requireStaffManagePermission(User currentUser, Space space) {
        if (!canManageSpace(currentUser, space)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to manage exams for this space");
        }
    }

    private boolean canManageSpace(User currentUser, Space space) {
        return hasRole(currentUser, RoleName.ADMIN)
                || hasRole(currentUser, RoleName.LECTURER) && space.getCreatedByUserId().equals(currentUser.getId());
    }

    private boolean hasRole(User user, RoleName roleName) {
        return user.getRoles().stream().anyMatch(role -> role.getName() == roleName);
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private Space getSpaceOrThrow(UUID spaceId) {
        return spaceRepository.findById(spaceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Space not found"));
    }

    private Exam getExamOrThrow(UUID examId) {
        return examRepository.findById(examId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exam not found"));
    }
}