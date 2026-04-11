package edusecure.edusecure.dto.attendance;

import edusecure.edusecure.entity.attendance.AttendanceStatus;

import java.time.Instant;
import java.util.UUID;

public record AttendanceStudentRecordResponse(
        UUID studentUserId,
        String studentEmail,
        String studentFullName,
        AttendanceStatus status,
        UUID recordedByUserId,
        Instant recordedAt
) {
}

