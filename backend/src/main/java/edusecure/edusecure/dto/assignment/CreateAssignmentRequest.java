package edusecure.edusecure.dto.assignment;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record CreateAssignmentRequest(
        @NotBlank(message = "Title is required")
        String title,
        @NotBlank(message = "Description is required")
        String description,
        @NotNull(message = "Due date is required")
        @Future(message = "Due date must be in the future")
        Instant dueAt,
        @NotNull(message = "Space is required")
        UUID spaceId
) {
}

