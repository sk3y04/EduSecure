package edusecure.edusecure.dto.grade;

import java.time.Instant;
import java.util.UUID;

public record GradeResponse(
        UUID id,
        UUID submissionId,
        Integer value,
        String feedback,
        UUID gradedByLecturerId,
        Instant gradedAt,
        Instant lastModifiedAt
) {
}

