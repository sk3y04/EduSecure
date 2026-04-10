package edusecure.edusecure.repository.registration;

import edusecure.edusecure.entity.registration.RegistrationRequestStatus;
import edusecure.edusecure.entity.registration.SpaceRegistrationRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface SpaceRegistrationRequestRepository extends JpaRepository<SpaceRegistrationRequest, UUID> {

    boolean existsBySpaceIdAndStudentUserIdAndStatus(
            UUID spaceId,
            UUID studentUserId,
            RegistrationRequestStatus status
    );

    List<SpaceRegistrationRequest> findAllByStudentUserIdOrderByRequestedAtDesc(UUID studentUserId);

    List<SpaceRegistrationRequest> findAllByStatusOrderByRequestedAtAsc(RegistrationRequestStatus status);

    @Query("""
            select request
            from SpaceRegistrationRequest request
            join Space space on space.id = request.spaceId
            where request.status = :status
              and space.createdByUserId = :lecturerUserId
            order by request.requestedAt asc
            """)
    List<SpaceRegistrationRequest> findReviewQueueByLecturerUserId(
            @Param("lecturerUserId") UUID lecturerUserId,
            @Param("status") RegistrationRequestStatus status
    );
}