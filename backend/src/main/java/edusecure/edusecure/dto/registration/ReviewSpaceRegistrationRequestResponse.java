package edusecure.edusecure.dto.registration;

import edusecure.edusecure.entity.registration.RegistrationRequestStatus;

import java.time.Instant;
import java.util.UUID;

public record ReviewSpaceRegistrationRequestResponse(
        UUID id,
        UUID spaceId,
        String spaceCode,
        String spaceName,
        UUID studentUserId,
        String studentEmail,
        String studentFullName,
        RegistrationRequestStatus status,
        String requestMessage,
        Instant requestedAt,
        Instant reviewedAt,
        String reviewNote,
        boolean canReview
) {
}