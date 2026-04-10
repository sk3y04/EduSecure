package edusecure.edusecure.dto.registration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateSpaceRegistrationRequest(
        @NotBlank(message = "Space code is required")
        @Size(min = 3, max = 32, message = "Space code must be between 3 and 32 characters")
        @Pattern(regexp = "^[A-Za-z0-9-]+$", message = "Space code may contain only letters, numbers, and hyphens")
        String spaceCode,
        @Size(max = 500, message = "Request message must be 500 characters or fewer")
        String requestMessage
) {
}