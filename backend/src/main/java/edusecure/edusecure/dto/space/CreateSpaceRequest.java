package edusecure.edusecure.dto.space;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateSpaceRequest(
        @NotBlank(message = "Space name is required")
        @Size(min = 3, max = 120, message = "Space name must be between 3 and 120 characters")
        String name,
        @NotBlank(message = "Space code is required")
        @Size(min = 3, max = 32, message = "Space code must be between 3 and 32 characters")
        @Pattern(regexp = "^[A-Za-z0-9-]+$", message = "Space code may contain only letters, numbers, and hyphens")
        String code,
        @NotBlank(message = "Description is required")
        @Size(min = 10, max = 2000, message = "Description must be between 10 and 2000 characters")
        String description
) {
}