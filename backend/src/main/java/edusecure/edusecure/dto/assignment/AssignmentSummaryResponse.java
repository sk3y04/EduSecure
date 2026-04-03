package edusecure.edusecure.dto.assignment;

import java.time.Instant;
import java.util.UUID;

public record AssignmentSummaryResponse(
        UUID id,
        String title,
        Instant dueAt,
        boolean open,
        UUID latestSubmissionId,
        Instant latestSubmittedAt
) {
}

