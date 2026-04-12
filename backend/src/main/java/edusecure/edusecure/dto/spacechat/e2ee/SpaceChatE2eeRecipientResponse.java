package edusecure.edusecure.dto.spacechat.e2ee;

import java.util.UUID;

public record SpaceChatE2eeRecipientResponse(
        UUID userId,
        String displayName,
        boolean manager,
        boolean keyRegistered,
        String algorithm,
        String publicKeyJwk,
        String fingerprint
) {
}

