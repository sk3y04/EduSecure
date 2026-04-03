package edusecure.edusecure.repository.grade;

import edusecure.edusecure.entity.grade.Grade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GradeRepository extends JpaRepository<Grade, UUID> {

    Optional<Grade> findBySubmissionId(UUID submissionId);

    List<Grade> findAllBySubmissionIdIn(Collection<UUID> submissionIds);
}