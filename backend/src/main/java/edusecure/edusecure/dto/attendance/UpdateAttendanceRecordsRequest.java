package edusecure.edusecure.dto.attendance;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UpdateAttendanceRecordsRequest(
        @NotNull(message = "Attendance records are required")
        List<@Valid AttendanceRecordRequest> records
) {
}

