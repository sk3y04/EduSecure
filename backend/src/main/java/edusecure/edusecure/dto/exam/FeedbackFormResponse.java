package edusecure.edusecure.dto.exam;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record FeedbackFormResponse(
        UUID id,
        UUID examId,
        String examTitle,
        UUID spaceId,
        String spaceCode,
        String spaceName,
        String title,
        String description,
        boolean published,
        Instant createdAt,
        Instant updatedAt,
        boolean canManage,
        boolean alreadySubmitted,
        Long responseCount,
        List<FeedbackFormQuestionResponse> questions
) {
}