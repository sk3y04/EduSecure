package edusecure.edusecure.repository.attendance;

import edusecure.edusecure.entity.attendance.AttendanceSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AttendanceSessionRepository extends JpaRepository<AttendanceSession, UUID> {

    List<AttendanceSession> findAllByOrderByStartsAtDesc();

    List<AttendanceSession> findAllBySpaceIdInOrderByStartsAtDesc(List<UUID> spaceIds);
}

