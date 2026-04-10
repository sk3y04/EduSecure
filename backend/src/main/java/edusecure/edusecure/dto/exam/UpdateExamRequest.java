package edusecure.edusecure.dto.exam;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public record UpdateExamRequest(
        @NotNull(message = "Space is required")
        UUID spaceId,
        @NotBlank(message = "Exam title is required")
        @Size(min = 3, max = 160, message = "Exam title must be between 3 and 160 characters")
        String title,
        @Size(max = 2000, message = "Description must be 2000 characters or fewer")
        String description,
        @NotBlank(message = "Location is required")
        @Size(min = 2, max = 160, message = "Location must be between 2 and 160 characters")
        String location,
        @NotNull(message = "Start time is required")
        Instant startsAt,
        @NotNull(message = "End time is required")
        Instant endsAt,
        boolean published
) {
}