package edusecure.edusecure.dto.spacechat.e2ee;

import java.time.Instant;

public record CurrentUserChatKeyResponse(
        boolean e2eeEnabled,
        boolean keyRegistrationRequired,
        boolean keyRegistered,
        String algorithm,
        String publicKeyJwk,
        String fingerprint,
        Instant createdAt
) {
}

