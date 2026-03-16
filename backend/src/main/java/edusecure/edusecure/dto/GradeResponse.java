package edusecure.edusecure.dto;

import java.time.Instant;
import java.util.UUID;

public record GradeResponse(
        UUID id,
        UUID submissionId,
        String value,
        String feedback,
        UUID gradedByLecturerId,
        Instant gradedAt,
        Instant lastModifiedAt
) {
}

