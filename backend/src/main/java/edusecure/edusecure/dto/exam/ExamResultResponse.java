package edusecure.edusecure.dto.exam;

import java.time.Instant;
import java.util.UUID;

public record ExamResultResponse(
        UUID id,
        UUID examId,
        String examTitle,
        UUID spaceId,
        String spaceCode,
        String spaceName,
        UUID studentUserId,
        String studentEmail,
        String studentFullName,
        Integer value,
        String feedback,
        boolean published,
        UUID gradedByUserId,
        Instant gradedAt,
        Instant lastModifiedAt,
        Instant publishedAt
) {
}