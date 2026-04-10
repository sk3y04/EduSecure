package edusecure.edusecure.dto.exam;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateExamResultRequest(
        @NotNull(message = "Result value is required")
        @Min(value = 0, message = "Result value must be between 0 and 100")
        @Max(value = 100, message = "Result value must be between 0 and 100")
        Integer value,
        @Size(max = 2000, message = "Feedback must be 2000 characters or fewer")
        String feedback,
        boolean published
) {
}