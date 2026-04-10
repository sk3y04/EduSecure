package edusecure.edusecure.dto.registration;

import jakarta.validation.constraints.Size;

public record ReviewSpaceRegistrationRequest(
        @Size(max = 500, message = "Review note must be 500 characters or fewer")
        String reviewNote
) {
}