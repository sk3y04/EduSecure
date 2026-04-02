package edusecure.edusecure.dto.space;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AddSpaceStudentRequest(
        @NotBlank(message = "Student email is required")
        @Email(message = "Student email must be a valid email address")
        String studentEmail
) {
}