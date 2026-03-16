package edusecure.edusecure.dto;

import java.time.Instant;
import java.util.UUID;

public record MyGradeResponse(
        UUID id,
        UUID submissionId,
        String value,
        String feedback,
        Instant lastModifiedAt
) {
}

