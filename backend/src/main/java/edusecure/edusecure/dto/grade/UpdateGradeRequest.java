package edusecure.edusecure.dto.grade;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateGradeRequest(
        @NotNull(message = "Grade percentage is required")
        @Min(value = 0, message = "Grade percentage must be between 0 and 100")
        @Max(value = 100, message = "Grade percentage must be between 0 and 100")
        Integer value,
        @NotBlank(message = "Feedback is required")
        String feedback
) {
}

