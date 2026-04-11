package edusecure.edusecure.repository.exam;

import edusecure.edusecure.entity.exam.FeedbackFormSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FeedbackFormSubmissionRepository extends JpaRepository<FeedbackFormSubmission, UUID> {

    Optional<FeedbackFormSubmission> findByFormIdAndStudentUserId(UUID formId, UUID studentUserId);

    List<FeedbackFormSubmission> findAllByFormIdOrderBySubmittedAtAsc(UUID formId);

    boolean existsByFormId(UUID formId);

    long countByFormId(UUID formId);
}