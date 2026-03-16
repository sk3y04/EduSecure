package edusecure.edusecure.dto;

import java.time.Instant;
import java.util.UUID;

public record AssignmentResponse(
        UUID id,
        String title,
        String description,
        Instant dueAt,
        UUID createdByLecturerId,
        boolean open
) {
}

