package edusecure.edusecure.repository.submission;

import edusecure.edusecure.entity.submission.Submission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SubmissionRepository extends JpaRepository<Submission, UUID> {

    Optional<Submission> findByIdAndStudentUserId(UUID id, UUID studentUserId);
}