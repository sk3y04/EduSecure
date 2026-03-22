package edusecure.edusecure.service.grade;

import edusecure.edusecure.audit.AuditService;
import edusecure.edusecure.dto.grade.CreateGradeRequest;
import edusecure.edusecure.dto.grade.GradeResponse;
import edusecure.edusecure.dto.grade.MyGradeResponse;
import edusecure.edusecure.dto.grade.UpdateGradeRequest;
import edusecure.edusecure.entity.audit.AuditActionType;
import edusecure.edusecure.entity.auth.RoleName;
import edusecure.edusecure.entity.auth.User;
import edusecure.edusecure.entity.grade.Grade;
import edusecure.edusecure.entity.submission.Submission;
import edusecure.edusecure.entity.submission.SubmissionVerificationStatus;
import edusecure.edusecure.repository.auth.UserRepository;
import edusecure.edusecure.repository.grade.GradeRepository;
import edusecure.edusecure.repository.submission.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GradeService {

    private final GradeRepository gradeRepository;
    private final SubmissionRepository submissionRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    @Transactional
    public GradeResponse createGrade(String currentUserEmail, UUID submissionId, CreateGradeRequest request) {
        User actor = findUserByEmail(currentUserEmail);
        Submission submission = findSubmission(submissionId);
        ensureSubmissionVerified(submission);

        if (gradeRepository.findBySubmissionId(submissionId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A grade already exists for this submission");
        }

        Grade grade = Grade.builder()
                .submissionId(submissionId)
                .value(request.value().trim())
                .feedback(request.feedback().trim())
                .gradedByLecturerId(actor.getId())
                .gradedAt(Instant.now())
                .build();

        Grade saved = gradeRepository.save(grade);
        auditService.record(
                AuditActionType.GRADE_CREATED,
                actor.getId(),
                Grade.class.getSimpleName(),
                saved.getId(),
                "submissionId=" + submissionId + ",value=" + saved.getValue()
        );
        return toGradeResponse(saved);
    }

    @Transactional
    public GradeResponse updateGrade(String currentUserEmail, UUID gradeId, UpdateGradeRequest request) {
        User actor = findUserByEmail(currentUserEmail);
        Grade grade = gradeRepository.findById(gradeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Grade not found"));
        Submission submission = findSubmission(grade.getSubmissionId());
        ensureSubmissionVerified(submission);

        grade.setValue(request.value().trim());
        grade.setFeedback(request.feedback().trim());
        grade.setGradedByLecturerId(actor.getId());
        grade.setLastModifiedAt(Instant.now());

        Grade saved = gradeRepository.save(grade);
        auditService.record(
                AuditActionType.GRADE_UPDATED,
                actor.getId(),
                Grade.class.getSimpleName(),
                saved.getId(),
                "submissionId=" + saved.getSubmissionId() + ",value=" + saved.getValue()
        );
        return toGradeResponse(saved);
    }

    @Transactional(readOnly = true)
    public GradeResponse getGrade(UUID gradeId, Authentication authentication) {
        Grade grade = gradeRepository.findById(gradeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Grade not found"));

        boolean privileged = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.equals("ROLE_" + RoleName.LECTURER.name())
                        || authority.equals("ROLE_" + RoleName.ADMIN.name()));

        if (!privileged) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot view this grade");
        }

        return toGradeResponse(grade);
    }

    @Transactional(readOnly = true)
    public MyGradeResponse getMyGrade(UUID gradeId, Authentication authentication) {
        User student = findUserByEmail(authentication.getName());
        Grade grade = gradeRepository.findById(gradeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Grade not found"));
        Submission submission = findSubmission(grade.getSubmissionId());

        if (!submission.getStudentUserId().equals(student.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot view this grade");
        }

        return new MyGradeResponse(
                grade.getId(),
                grade.getSubmissionId(),
                grade.getValue(),
                grade.getFeedback(),
                grade.getLastModifiedAt()
        );
    }

    private GradeResponse toGradeResponse(Grade grade) {
        return new GradeResponse(
                grade.getId(),
                grade.getSubmissionId(),
                grade.getValue(),
                grade.getFeedback(),
                grade.getGradedByLecturerId(),
                grade.getGradedAt(),
                grade.getLastModifiedAt()
        );
    }

    private void ensureSubmissionVerified(Submission submission) {
        if (submission.getVerificationStatus() != SubmissionVerificationStatus.VERIFIED) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Only verified submissions can be graded");
        }
    }

    private Submission findSubmission(UUID submissionId) {
        return submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Submission not found"));
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }
}

