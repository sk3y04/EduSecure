package edusecure.edusecure.repository.attendance;

import edusecure.edusecure.entity.attendance.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, UUID> {

    List<AttendanceRecord> findAllBySessionId(UUID sessionId);

    List<AttendanceRecord> findAllBySessionIdIn(List<UUID> sessionIds);

    List<AttendanceRecord> findAllByStudentUserIdAndSessionIdIn(UUID studentUserId, List<UUID> sessionIds);
}

