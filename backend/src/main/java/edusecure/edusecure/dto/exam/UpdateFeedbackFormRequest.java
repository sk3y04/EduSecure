package edusecure.edusecure.dto.exam;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateFeedbackFormRequest(
        @NotBlank(message = "Form title is required")
        @Size(min = 3, max = 160, message = "Form title must be between 3 and 160 characters")
        String title,
        @Size(max = 2000, message = "Description must be 2000 characters or fewer")
        String description,
        boolean published,
        @NotEmpty(message = "At least one question is required")
        @Size(max = 10, message = "A form may contain at most 10 questions")
        List<@Valid CreateFeedbackFormQuestionRequest> questions
) {
}