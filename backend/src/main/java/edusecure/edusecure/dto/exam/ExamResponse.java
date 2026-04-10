package edusecure.edusecure.dto.exam;

import java.time.Instant;
import java.util.UUID;

public record ExamResponse(
        UUID id,
        UUID spaceId,
        String spaceCode,
        String spaceName,
        String title,
        String description,
        String location,
        Instant startsAt,
        Instant endsAt,
        boolean published,
        UUID createdByUserId,
        Instant createdAt,
        Instant updatedAt,
        boolean canManage
) {
}