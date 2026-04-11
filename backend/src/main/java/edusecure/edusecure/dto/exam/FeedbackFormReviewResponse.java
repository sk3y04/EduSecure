package edusecure.edusecure.dto.exam;

import java.util.List;
import java.util.UUID;

public record FeedbackFormReviewResponse(
        UUID formId,
        String title,
        String examTitle,
        String spaceCode,
        String spaceName,
        long responseCount,
        List<FeedbackFormQuestionSummaryResponse> questionSummaries,
        List<FeedbackFormSubmissionResponse> submissions
) {
}