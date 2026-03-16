package edusecure.edusecure.dto;

import edusecure.edusecure.entity.SubmissionVerificationStatus;

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
        String verificationMessage
) {
}

