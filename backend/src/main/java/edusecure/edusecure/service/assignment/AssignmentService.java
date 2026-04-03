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

@Service
@RequiredArgsConstructor
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    @Transactional
    public AssignmentResponse createAssignment(String currentUserEmail, CreateAssignmentRequest request) {
        User lecturer = findUserByEmail(currentUserEmail);

        Assignment assignment = Assignment.builder()
                .title(request.title().trim())
                .description(request.description().trim())
                .dueAt(request.dueAt())
                .createdByLecturerId(lecturer.getId())
                .open(true)
                .build();

        Assignment saved = assignmentRepository.save(assignment);
        auditService.record(
                AuditActionType.ASSIGNMENT_CREATED,
                lecturer.getId(),
                Assignment.class.getSimpleName(),
                saved.getId(),
                "title=" + saved.getTitle()
        );
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<AssignmentSummaryResponse> listAssignments(Authentication authentication) {
        User currentUser = findUserByEmail(authentication.getName());
        boolean studentView = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> ("ROLE_" + RoleName.STUDENT.name()).equals(authority));

        return assignmentRepository.findAllByOrderByDueAtAsc().stream()
                .map(assignment -> {
                    java.util.UUID latestSubmissionId = null;
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
                            assignment.isOpen(),
                            latestSubmissionId,
                            latestSubmittedAt
                    );
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public Assignment getAssignmentOrThrow(java.util.UUID assignmentId) {
        return assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found"));
    }

    private AssignmentResponse toResponse(Assignment assignment) {
        return new AssignmentResponse(
                assignment.getId(),
                assignment.getTitle(),
                assignment.getDescription(),
                assignment.getDueAt(),
                assignment.getCreatedByLecturerId(),
                assignment.isOpen()
        );
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }
}