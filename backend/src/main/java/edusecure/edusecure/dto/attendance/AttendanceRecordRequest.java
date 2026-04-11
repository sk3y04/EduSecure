package edusecure.edusecure.dto.attendance;

import edusecure.edusecure.entity.attendance.AttendanceStatus;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AttendanceRecordRequest(
        @NotNull(message = "Student is required")
        UUID studentUserId,
        AttendanceStatus status
) {
}

