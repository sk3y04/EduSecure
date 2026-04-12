package edusecure.edusecure.service.spacechat;

import edusecure.edusecure.dto.spacechat.CreateSpaceChatMessageRequest;
import edusecure.edusecure.dto.spacechat.SpaceChatMessagePageResponse;
import edusecure.edusecure.dto.spacechat.SpaceChatMessageResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@ConditionalOnProperty(prefix = "app.chat", name = "enabled", havingValue = "false", matchIfMissing = true)
public class DisabledSpaceChatService implements SpaceChatService {

    private static final String CHAT_UNAVAILABLE_MESSAGE = "Space chat is temporarily unavailable";

    @Override
    public SpaceChatMessagePageResponse listMessages(String currentUserEmail, UUID spaceId, Integer limit, String beforeCreatedAt, String beforeMessageId) {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, CHAT_UNAVAILABLE_MESSAGE);
    }

    @Override
    public SpaceChatMessageResponse createMessage(String currentUserEmail, UUID spaceId, CreateSpaceChatMessageRequest request) {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, CHAT_UNAVAILABLE_MESSAGE);
    }
}


