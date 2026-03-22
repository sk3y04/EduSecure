package edusecure.edusecure.dto.auth;

import edusecure.edusecure.entity.auth.MfaMethod;

import java.time.Instant;

public record MfaStatusResponse(
        boolean mfaEnabled,
        MfaMethod mfaMethod,
        long recoveryCodesRemaining,
        Instant enabledAt
) {
}

