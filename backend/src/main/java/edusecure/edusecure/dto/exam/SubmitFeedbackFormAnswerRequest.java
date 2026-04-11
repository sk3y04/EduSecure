package edusecure.edusecure.dto.exam;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record SubmitFeedbackFormAnswerRequest(
        @NotNull(message = "Question id is required")
        UUID questionId,
        Integer ratingValue,
        @Size(max = 2000, message = "Text answers must be 2000 characters or fewer")
        String textValue
) {
}