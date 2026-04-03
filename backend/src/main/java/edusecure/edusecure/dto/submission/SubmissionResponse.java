package edusecure.edusecure.dto.submission;

import edusecure.edusecure.entity.submission.SubmissionVerificationStatus;

import java.time.Instant;
import java.util.UUID;

public record SubmissionResponse(
        UUID id,
        UUID assignmentId,
        UUID studentUserId,
        Instant submittedAt,
        String fileName,
        String contentType,
        String hashDigest,
        String digitalSignature,
        String signatureAlgorithm,
        SubmissionVerificationStatus verificationStatus,
        String verificationMessage,
        boolean graded,
        UUID gradeId
) {
}

