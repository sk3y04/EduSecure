package edusecure.edusecure.dto.exam;

import edusecure.edusecure.entity.exam.FeedbackQuestionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateFeedbackFormQuestionRequest(
        @NotBlank(message = "Question prompt is required")
        @Size(min = 3, max = 300, message = "Question prompt must be between 3 and 300 characters")
        String prompt,
        @NotNull(message = "Question type is required")
        FeedbackQuestionType questionType,
        boolean required,
        @NotNull(message = "Display order is required")
        Integer displayOrder
) {
}