package edusecure.edusecure.dto.exam;

import edusecure.edusecure.entity.exam.FeedbackQuestionType;

import java.util.Map;
import java.util.UUID;

public record FeedbackFormQuestionSummaryResponse(
        UUID questionId,
        String prompt,
        FeedbackQuestionType questionType,
        long responseCount,
        Double averageRating,
        Map<Integer, Long> ratingCounts
) {
}