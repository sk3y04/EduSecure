package edusecure.edusecure.repository.space;

import edusecure.edusecure.entity.space.SpaceMembership;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpaceMembershipRepository extends JpaRepository<SpaceMembership, UUID> {

    List<SpaceMembership> findAllBySpaceIdOrderByAddedAtAsc(UUID spaceId);

    List<SpaceMembership> findAllByStudentUserId(UUID studentUserId);

    Optional<SpaceMembership> findBySpaceIdAndStudentUserId(UUID spaceId, UUID studentUserId);

    boolean existsBySpaceIdAndStudentUserId(UUID spaceId, UUID studentUserId);

    long countBySpaceId(UUID spaceId);
}