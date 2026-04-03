package edusecure.edusecure.repository.submission;

import edusecure.edusecure.entity.submission.Submission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

public interface SubmissionRepository extends JpaRepository<Submission, UUID> {

    Optional<Submission> findByIdAndStudentUserId(UUID id, UUID studentUserId);

    Optional<Submission> findFirstByAssignmentIdAndStudentUserIdOrderBySubmittedAtDescIdDesc(UUID assignmentId, UUID studentUserId);

    List<Submission> findAllByAssignmentIdOrderBySubmittedAtDesc(UUID assignmentId);
}