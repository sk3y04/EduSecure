package edusecure.edusecure.dto;

import edusecure.edusecure.entity.MfaMethod;

import java.time.Instant;

public record MfaStatusResponse(
        boolean mfaEnabled,
        MfaMethod mfaMethod,
        long recoveryCodesRemaining,
        Instant enabledAt
) {
}

