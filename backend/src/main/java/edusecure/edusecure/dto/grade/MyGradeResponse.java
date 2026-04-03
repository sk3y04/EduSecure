package edusecure.edusecure.dto.grade;

import java.time.Instant;
import java.util.UUID;

public record MyGradeResponse(
        UUID id,
        UUID submissionId,
        Integer value,
        String feedback,
        Instant lastModifiedAt
) {
}

