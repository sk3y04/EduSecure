package edusecure.edusecure.repository.assignment;

import edusecure.edusecure.entity.assignment.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface AssignmentRepository extends JpaRepository<Assignment, UUID> {

    List<Assignment> findAllByOrderByDueAtAsc();

    List<Assignment> findAllByCreatedByLecturerIdOrderByDueAtAsc(UUID createdByLecturerId);

    List<Assignment> findAllBySpaceIdInOrderByDueAtAsc(Collection<UUID> spaceIds);
}