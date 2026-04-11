package edusecure.edusecure.dto.attendance;

import java.util.List;

public record AttendanceSessionRecordsResponse(
        AttendanceSessionResponse session,
        List<AttendanceStudentRecordResponse> records
) {
}

