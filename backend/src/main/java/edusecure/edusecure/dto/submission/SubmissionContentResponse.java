package edusecure.edusecure.dto.submission;

import java.util.UUID;

public record SubmissionContentResponse(
        UUID submissionId,
        String fileName,
        String contentType,
        String content
) {
}

