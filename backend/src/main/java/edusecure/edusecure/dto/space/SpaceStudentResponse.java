package edusecure.edusecure.dto.space;

import java.time.Instant;
import java.util.UUID;

public record SpaceStudentResponse(
        UUID studentUserId,
        String studentEmail,
        String studentFullName,
        UUID addedByUserId,
        Instant addedAt
) {
}