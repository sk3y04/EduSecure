package edusecure.edusecure.dto.attendance;

import edusecure.edusecure.entity.attendance.AttendanceStatus;

import java.time.Instant;
import java.util.UUID;

public record AttendanceSessionResponse(
        UUID id,
        UUID spaceId,
        String spaceCode,
        String spaceName,
        String title,
        String description,
        Instant startsAt,
        Instant endsAt,
        UUID createdByUserId,
        Instant createdAt,
        Instant updatedAt,
        boolean canManage,
        AttendanceStatus myStatus,
        long memberCount,
        long recordedCount,
        long presentCount,
        long lateCount,
        long absentCount,
        long excusedCount
) {
}

