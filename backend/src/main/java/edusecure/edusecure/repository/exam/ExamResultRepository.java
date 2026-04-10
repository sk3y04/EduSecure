package edusecure.edusecure.repository.exam;

import edusecure.edusecure.entity.exam.ExamResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExamResultRepository extends JpaRepository<ExamResult, UUID> {

    Optional<ExamResult> findByExamIdAndStudentUserId(UUID examId, UUID studentUserId);

    List<ExamResult> findAllByExamIdOrderByGradedAtAsc(UUID examId);

    List<ExamResult> findAllByStudentUserIdAndPublishedTrueOrderByPublishedAtDescGradedAtDesc(UUID studentUserId);
}