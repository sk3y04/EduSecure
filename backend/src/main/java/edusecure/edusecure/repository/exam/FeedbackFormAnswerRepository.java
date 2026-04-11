package edusecure.edusecure.repository.exam;

import edusecure.edusecure.entity.exam.FeedbackFormAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FeedbackFormAnswerRepository extends JpaRepository<FeedbackFormAnswer, UUID> {

    List<FeedbackFormAnswer> findAllBySubmissionIdIn(List<UUID> submissionIds);
}