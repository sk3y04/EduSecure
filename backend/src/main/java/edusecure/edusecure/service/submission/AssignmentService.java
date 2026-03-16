package edusecure.edusecure.service.submission;

import edusecure.edusecure.audit.AuditService;
import edusecure.edusecure.dto.AssignmentResponse;
import edusecure.edusecure.dto.AssignmentSummaryResponse;
import edusecure.edusecure.dto.CreateAssignmentRequest;
import edusecure.edusecure.entity.Assignment;
import edusecure.edusecure.entity.AuditActionType;
import edusecure.edusecure.entity.User;
import edusecure.edusecure.repository.AssignmentRepository;
import edusecure.edusecure.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
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
    public List<AssignmentSummaryResponse> listAssignments() {
        return assignmentRepository.findAllByOrderByDueAtAsc().stream()
                .map(assignment -> new AssignmentSummaryResponse(
                        assignment.getId(),
                        assignment.getTitle(),
                        assignment.getDueAt(),
                        assignment.isOpen()
                ))
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

