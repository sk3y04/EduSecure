package edusecure.edusecure.dto.spacechat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SpaceChatMessagePageResponse(
        List<SpaceChatMessageResponse> items,
        boolean hasMore,
        Instant nextCursorBeforeCreatedAt,
        UUID nextCursorBeforeMessageId
) {
}

