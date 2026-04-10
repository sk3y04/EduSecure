package edusecure.edusecure.repository.exam;

import edusecure.edusecure.entity.exam.Exam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ExamRepository extends JpaRepository<Exam, UUID> {

    List<Exam> findAllByOrderByStartsAtAsc();

    List<Exam> findAllBySpaceIdInOrderByStartsAtAsc(List<UUID> spaceIds);

    List<Exam> findAllBySpaceIdInAndPublishedTrueOrderByStartsAtAsc(List<UUID> spaceIds);

    @Query("""
            select count(e)
            from Exam e
            where e.spaceId = :spaceId
              and (:excludedExamId is null or e.id <> :excludedExamId)
              and e.startsAt < :endsAt
              and e.endsAt > :startsAt
            """)
    long countOverlappingExams(
            @Param("spaceId") UUID spaceId,
            @Param("startsAt") Instant startsAt,
            @Param("endsAt") Instant endsAt,
            @Param("excludedExamId") UUID excludedExamId
    );
}