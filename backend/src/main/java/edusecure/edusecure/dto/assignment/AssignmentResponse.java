package edusecure.edusecure.dto.assignment;

import java.time.Instant;
import java.util.UUID;

public record AssignmentResponse(
        UUID id,
        String title,
        String description,
        Instant dueAt,
        UUID createdByLecturerId,
        UUID spaceId,
        boolean open
) {
}

