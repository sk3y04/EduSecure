package edusecure.edusecure.service.spacechat;

import edusecure.edusecure.dto.spacechat.CreateSpaceChatMessageRequest;
import edusecure.edusecure.dto.spacechat.SpaceChatMessagePageResponse;
import edusecure.edusecure.dto.spacechat.SpaceChatMessageResponse;

import java.util.UUID;

public interface SpaceChatService {

    SpaceChatMessagePageResponse listMessages(
            String currentUserEmail,
            UUID spaceId,
            Integer limit,
            String beforeCreatedAt,
            String beforeMessageId
    );

    SpaceChatMessageResponse createMessage(String currentUserEmail, UUID spaceId, CreateSpaceChatMessageRequest request);
}

