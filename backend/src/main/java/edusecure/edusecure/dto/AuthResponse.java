package edusecure.edusecure.dto;

import edusecure.edusecure.entity.MfaMethod;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record AuthResponse(
        AuthStatus authStatus,
        UUID userId,
        String email,
        String fullName,
        Set<String> roles,
        String token,
        Boolean mfaEnabled,
        List<String> amr,
        UUID challengeId,
        MfaMethod mfaMethod,
        Instant expiresAt,
        Integer remainingAttempts
) {
    public AuthResponse withoutToken() {
        return new AuthResponse(
                authStatus,
                userId,
                email,
                fullName,
                roles,
                null,
                mfaEnabled,
                amr,
                challengeId,
                mfaMethod,
                expiresAt,
                remainingAttempts
        );
    }
}

