package edusecure.edusecure.dto.spacechat.e2ee;

import java.util.UUID;
import java.time.Instant;

public record SpaceChatWrappedKeyResponse(
        UUID publisherUserId,
        String publisherPublicKeyJwk,
        String publisherKeyFingerprint,
        String wrapAlgorithm,
        String wrapNonce,
        String wrappedKeyCiphertext,
        Instant createdAt
) {
}

