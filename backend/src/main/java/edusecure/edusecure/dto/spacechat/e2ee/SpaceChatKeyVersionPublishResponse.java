package edusecure.edusecure.dto.spacechat.e2ee;

import java.time.Instant;

public record SpaceChatKeyVersionPublishResponse(
        int keyVersion,
        String rotationReason,
        int recipientCount,
        boolean requiresRekey,
        String publisherKeyFingerprint,
        Instant createdAt
) {
}

