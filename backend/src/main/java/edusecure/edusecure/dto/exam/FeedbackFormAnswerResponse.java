package edusecure.edusecure.dto.exam;

import java.util.UUID;

public record FeedbackFormAnswerResponse(
        UUID questionId,
        Integer ratingValue,
        String textValue
) {
}