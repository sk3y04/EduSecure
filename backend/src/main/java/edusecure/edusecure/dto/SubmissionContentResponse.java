package edusecure.edusecure.dto;

import java.util.UUID;

public record SubmissionContentResponse(
        UUID submissionId,
        String fileName,
        String contentType,
        String content
) {
}

