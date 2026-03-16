package edusecure.edusecure.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateGradeRequest(
        @NotBlank(message = "Grade value is required")
        String value,
        @NotBlank(message = "Feedback is required")
        String feedback
) {
}

