package edusecure.edusecure.dto.exam;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SubmitFeedbackFormRequest(
        @NotEmpty(message = "At least one answer is required")
        List<@Valid SubmitFeedbackFormAnswerRequest> answers
) {
}