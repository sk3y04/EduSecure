package edusecure.edusecure.dto.registration;

import edusecure.edusecure.entity.registration.RegistrationRequestStatus;

import java.time.Instant;
import java.util.UUID;

public record StudentSpaceRegistrationRequestResponse(
        UUID id,
        UUID spaceId,
        String spaceCode,
        String spaceName,
        RegistrationRequestStatus status,
        String requestMessage,
        Instant requestedAt,
        Instant reviewedAt,
        String reviewNote
) {
}