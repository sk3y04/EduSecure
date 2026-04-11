package edusecure.edusecure.repository.exam;

import edusecure.edusecure.entity.exam.FeedbackFormQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FeedbackFormQuestionRepository extends JpaRepository<FeedbackFormQuestion, UUID> {

    List<FeedbackFormQuestion> findAllByFormIdOrderByDisplayOrderAsc(UUID formId);

    List<FeedbackFormQuestion> findAllByFormIdInOrderByDisplayOrderAsc(List<UUID> formIds);

    void deleteAllByFormId(UUID formId);
}