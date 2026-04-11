package edusecure.edusecure.dto.exam;

import edusecure.edusecure.entity.exam.FeedbackQuestionType;

import java.util.UUID;

public record FeedbackFormQuestionResponse(
        UUID id,
        String prompt,
        FeedbackQuestionType questionType,
        boolean required,
        Integer displayOrder
) {
}