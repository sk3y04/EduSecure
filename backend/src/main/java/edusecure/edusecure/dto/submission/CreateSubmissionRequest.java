package edusecure.edusecure.dto.submission;

import jakarta.validation.constraints.NotBlank;

public record CreateSubmissionRequest(
        @NotBlank(message = "File name is required")
        String fileName,
        @NotBlank(message = "Content type is required")
        String contentType,
        @NotBlank(message = "Submission content is required")
        String content
) {
}

