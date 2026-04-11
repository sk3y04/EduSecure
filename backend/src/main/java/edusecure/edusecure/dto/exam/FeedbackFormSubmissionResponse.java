package edusecure.edusecure.dto.exam;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record FeedbackFormSubmissionResponse(
        UUID id,
        UUID studentUserId,
        String studentEmail,
        String studentFullName,
        Instant submittedAt,
        List<FeedbackFormAnswerResponse> answers
) {
}