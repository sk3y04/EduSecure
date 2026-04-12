package edusecure.edusecure.dto.spacechat;

import java.time.Instant;
import java.util.UUID;

public record SpaceChatMessageResponse(
        UUID id,
        UUID spaceId,
        UUID authorUserId,
        String authorDisplayName,
        String body,
        Integer keyVersion,
        String algorithm,
        String nonce,
        String ciphertext,
        String contentType,
        Integer plaintextLength,
        Instant createdAt
) {
}

