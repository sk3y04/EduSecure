package edusecure.edusecure.service.assignment;

import edusecure.edusecure.audit.AuditService;
import edusecure.edusecure.dto.assignment.AssignmentResponse;
import edusecure.edusecure.dto.assignment.AssignmentSummaryResponse;
import edusecure.edusecure.dto.assignment.CreateAssignmentRequest;
import edusecure.edusecure.entity.assignment.Assignment;
import edusecure.edusecure.entity.audit.AuditActionType;
import edusecure.edusecure.entity.auth.RoleName;
import edusecure.edusecure.entity.auth.User;
import edusecure.edusecure.repository.assignment.AssignmentRepository;
import edusecure.edusecure.repository.space.SpaceMembershipRepository;
import edusecure.edusecure.repository.space.SpaceRepository;
import edusecure.edusecure.repository.submission.SubmissionRepository;
import edusecure.edusecure.repository.auth.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final SpaceRepository spaceRepository;
    private final SpaceMembershipRepository spaceMembershipRepository;
    private final SubmissionRepository submissionRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    @Transactional
    public AssignmentResponse createAssignment(String currentUserEmail, CreateAssignmentRequest request) {
        User actor = findUserByEmail(currentUserEmail);
        boolean admin = hasRole(actor, RoleName.ADMIN);
        boolean lecturer = hasRole(actor, RoleName.LECTURER);
        if (!admin && !lecturer) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to create assignments");
        }

        var space = spaceRepository.findById(request.spaceId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Space not found"));

        if (!admin && !space.getCreatedByUserId().equals(actor.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to create assignments for this space");
        }

        Assignment assignment = Assignment.builder()
                .title(request.title().trim())
                .description(request.description().trim())
                .dueAt(request.dueAt())
                .createdByLecturerId(actor.getId())
                .spaceId(space.getId())
                .open(true)
                .build();

        Assignment saved = assignmentRepository.save(assignment);
        auditService.record(
                AuditActionType.ASSIGNMENT_CREATED,
                actor.getId(),
                Assignment.class.getSimpleName(),
                saved.getId(),
                "title=" + saved.getTitle() + ",spaceId=" + saved.getSpaceId()
        );
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<AssignmentSummaryResponse> listAssignments(Authentication authentication) {
        User currentUser = findUserByEmail(authentication.getName());
        boolean studentView = hasRole(authentication, RoleName.STUDENT);

        List<Assignment> assignments = resolveVisibleAssignments(authentication, currentUser);

        return assignments.stream()
                .map(assignment -> {
                    UUID latestSubmissionId = null;
                    java.time.Instant latestSubmittedAt = null;

                    if (studentView) {
                        var latestSubmission = submissionRepository
                                .findFirstByAssignmentIdAndStudentUserIdOrderBySubmittedAtDescIdDesc(assignment.getId(), currentUser.getId())
                                .orElse(null);

                        if (latestSubmission != null) {
                            latestSubmissionId = latestSubmission.getId();
                            latestSubmittedAt = latestSubmission.getSubmittedAt();
                        }
                    }

                    return new AssignmentSummaryResponse(
                            assignment.getId(),
                            assignment.getTitle(),
                            assignment.getDueAt(),
                            assignment.getSpaceId(),
                            assignment.isOpen(),
                            latestSubmissionId,
                            latestSubmittedAt
                    );
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public Assignment getAssignmentOrThrow(UUID assignmentId) {
        return assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found"));
    }

    @Transactional(readOnly = true)
    public Assignment getStudentAccessibleAssignmentOrThrow(String studentEmail, UUID assignmentId) {
        User student = findUserByEmail(studentEmail);
        return getStudentAccessibleAssignmentOrThrow(student.getId(), assignmentId);
    }

    @Transactional(readOnly = true)
    public Assignment getStudentAccessibleAssignmentOrThrow(UUID studentUserId, UUID assignmentId) {
        Assignment assignment = getAssignmentOrThrow(assignmentId);
        if (!isVisibleToStudent(studentUserId, assignment)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot access this assignment");
        }
        return assignment;
    }

    @Transactional(readOnly = true)
    public Assignment getLecturerOrAdminAccessibleAssignmentOrThrow(
            UUID assignmentId,
            User currentUser,
            Authentication authentication,
            String forbiddenMessage
    ) {
        Assignment assignment = getAssignmentOrThrow(assignmentId);
        requireLecturerOrAdminAssignmentAccess(assignment, currentUser, authentication, forbiddenMessage);
        return assignment;
    }

    public void requireLecturerOrAdminAssignmentAccess(
            Assignment assignment,
            User currentUser,
            Authentication authentication,
            String forbiddenMessage
    ) {
        if (hasRole(authentication, RoleName.ADMIN)) {
            return;
        }

        if (hasRole(authentication, RoleName.LECTURER) && assignment.getCreatedByLecturerId().equals(currentUser.getId())) {
            return;
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, forbiddenMessage);
    }

    private AssignmentResponse toResponse(Assignment assignment) {
        return new AssignmentResponse(
                assignment.getId(),
                assignment.getTitle(),
                assignment.getDescription(),
                assignment.getDueAt(),
                assignment.getCreatedByLecturerId(),
                assignment.getSpaceId(),
                assignment.isOpen()
        );
    }

    private List<Assignment> resolveVisibleAssignments(Authentication authentication, User currentUser) {
        if (hasRole(authentication, RoleName.ADMIN)) {
            return assignmentRepository.findAllByOrderByDueAtAsc();
        }

        if (hasRole(authentication, RoleName.LECTURER)) {
            return assignmentRepository.findAllByCreatedByLecturerIdOrderByDueAtAsc(currentUser.getId());
        }

        if (hasRole(authentication, RoleName.STUDENT)) {
            List<UUID> spaceIds = spaceMembershipRepository.findAllByStudentUserId(currentUser.getId()).stream()
                    .map(membership -> membership.getSpaceId())
                    .distinct()
                    .toList();

            if (spaceIds.isEmpty()) {
                return List.of();
            }

            return assignmentRepository.findAllBySpaceIdInOrderByDueAtAsc(spaceIds);
        }

        return List.of();
    }

    public boolean isVisibleToStudent(UUID studentUserId, Assignment assignment) {
        return assignment.getSpaceId() != null
                && spaceMembershipRepository.existsBySpaceIdAndStudentUserId(assignment.getSpaceId(), studentUserId);
    }

    private boolean hasRole(Authentication authentication, RoleName roleName) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> ("ROLE_" + roleName.name()).equals(authority));
    }

    private boolean hasRole(User user, RoleName roleName) {
        return user.getRoles().stream().anyMatch(role -> role.getName() == roleName);
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }
}