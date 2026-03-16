package edusecure.edusecure.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,
        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        @Pattern(regexp = ".*[A-Z].*", message = "Password must include at least one uppercase letter")
        @Pattern(regexp = ".*[a-z].*", message = "Password must include at least one lowercase letter")
        @Pattern(regexp = ".*\\d.*", message = "Password must include at least one number")
        @Pattern(regexp = ".*[^A-Za-z0-9].*", message = "Password must include at least one special character")
        String password,
        @NotBlank(message = "Full name is required")
        String fullName
) {
}

