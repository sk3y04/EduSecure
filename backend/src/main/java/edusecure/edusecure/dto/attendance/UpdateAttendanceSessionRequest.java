package edusecure.edusecure.dto.attendance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record UpdateAttendanceSessionRequest(
        @NotBlank(message = "Session title is required")
        @Size(min = 3, max = 160, message = "Session title must be between 3 and 160 characters")
        String title,
        @Size(max = 1000, message = "Description must be 1000 characters or fewer")
        String description,
        @NotNull(message = "Start time is required")
        Instant startsAt,
        @NotNull(message = "End time is required")
        Instant endsAt
) {
}

