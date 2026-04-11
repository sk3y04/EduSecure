package edusecure.edusecure.repository.exam;

import edusecure.edusecure.entity.exam.FeedbackForm;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FeedbackFormRepository extends JpaRepository<FeedbackForm, UUID> {

    List<FeedbackForm> findAllByExamIdOrderByCreatedAtDesc(UUID examId);

    List<FeedbackForm> findAllByExamIdAndPublishedTrueOrderByCreatedAtDesc(UUID examId);
}