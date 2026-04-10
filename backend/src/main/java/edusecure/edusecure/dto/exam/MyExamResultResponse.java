package edusecure.edusecure.dto.exam;

import java.time.Instant;
import java.util.UUID;

public record MyExamResultResponse(
        UUID id,
        UUID examId,
        String examTitle,
        String spaceCode,
        String spaceName,
        Integer value,
        String feedback,
        Instant publishedAt,
        Instant lastModifiedAt
) {
}