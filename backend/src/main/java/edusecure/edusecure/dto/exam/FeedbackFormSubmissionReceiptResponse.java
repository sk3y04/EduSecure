package edusecure.edusecure.dto.exam;

import java.time.Instant;
import java.util.UUID;

public record FeedbackFormSubmissionReceiptResponse(
        UUID submissionId,
        UUID formId,
        Instant submittedAt
) {
}