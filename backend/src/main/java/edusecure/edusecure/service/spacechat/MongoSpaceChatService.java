package edusecure.edusecure.service.spacechat;

import com.mongodb.MongoException;
import edusecure.edusecure.audit.AuditService;
import edusecure.edusecure.config.chat.SpaceChatProperties;
import edusecure.edusecure.document.spacechat.SpaceChatMessage;
import edusecure.edusecure.dto.spacechat.CreateSpaceChatMessageRequest;
import edusecure.edusecure.dto.spacechat.SpaceChatMessagePageResponse;
import edusecure.edusecure.dto.spacechat.SpaceChatMessageResponse;
import edusecure.edusecure.entity.audit.AuditActionType;
import edusecure.edusecure.repository.spacechat.SpaceChatMessageRepository;
import edusecure.edusecure.service.space.SpaceAccessContext;
import edusecure.edusecure.service.space.SpaceAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.chat", name = "enabled", havingValue = "true")
public class MongoSpaceChatService implements SpaceChatService {

    private static final String CHAT_UNAVAILABLE_MESSAGE = "Space chat is temporarily unavailable";

    private final SpaceAccessService spaceAccessService;
    private final SpaceChatMessageRepository spaceChatMessageRepository;
    private final MongoTemplate mongoTemplate;
    private final AuditService auditService;
    private final SpaceChatProperties spaceChatProperties;

    @Override
    public SpaceChatMessagePageResponse listMessages(
            String currentUserEmail,
            UUID spaceId,
            Integer limit,
            String beforeCreatedAt,
            String beforeMessageId
    ) {
        spaceAccessService.requireReadableSpace(currentUserEmail, spaceId);
        ChatCursor cursor = parseCursor(beforeCreatedAt, beforeMessageId);
        int resolvedLimit = resolveLimit(limit);

        try {
            Query query = new Query()
                    .addCriteria(Criteria.where("spaceId").is(spaceId.toString()))
                    .with(Sort.by(
                            Sort.Order.desc("createdAt"),
                            Sort.Order.desc("_id")
                    ))
                    .limit(resolvedLimit + 1);

            if (cursor != null) {
                query.addCriteria(new Criteria().orOperator(
                        Criteria.where("createdAt").lt(cursor.createdAt()),
                        new Criteria().andOperator(
                                Criteria.where("createdAt").is(cursor.createdAt()),
                                Criteria.where("_id").lt(cursor.messageId().toString())
                        )
                ));
            }

            List<SpaceChatMessage> queriedMessages = mongoTemplate.find(query, SpaceChatMessage.class);
            boolean hasMore = queriedMessages.size() > resolvedLimit;
            if (hasMore) {
                queriedMessages = new ArrayList<>(queriedMessages.subList(0, resolvedLimit));
            }

            Collections.reverse(queriedMessages);
            List<SpaceChatMessageResponse> items = queriedMessages.stream()
                    .map(this::toResponse)
                    .toList();

            SpaceChatMessageResponse oldestItem = items.isEmpty() ? null : items.get(0);
            return new SpaceChatMessagePageResponse(
                    items,
                    hasMore,
                    hasMore && oldestItem != null ? oldestItem.createdAt() : null,
                    hasMore && oldestItem != null ? oldestItem.id() : null
            );
        } catch (MongoException | DataAccessException ex) {
            throw unavailable(ex);
        }
    }

    @Override
    public SpaceChatMessageResponse createMessage(String currentUserEmail, UUID spaceId, CreateSpaceChatMessageRequest request) {
        SpaceAccessContext accessContext = spaceAccessService.requireWritableChatSpace(currentUserEmail, spaceId);
        String trimmedBody = normalizeBody(request.body());

        if (trimmedBody.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message body is required");
        }

        if (trimmedBody.length() > spaceChatProperties.getMessageMaxLength()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Message body must not exceed " + spaceChatProperties.getMessageMaxLength() + " characters"
            );
        }

        UUID messageId = UUID.randomUUID();
        Instant createdAt = Instant.now();
        SpaceChatMessage message = SpaceChatMessage.builder()
                .id(messageId.toString())
                .spaceId(spaceId.toString())
                .authorUserId(accessContext.currentUser().getId().toString())
                .authorDisplayName(accessContext.currentUser().getFullName())
                .body(trimmedBody)
                .createdAt(createdAt)
                .build();

        try {
            SpaceChatMessage saved = spaceChatMessageRepository.save(message);
            auditService.record(
                    AuditActionType.SPACE_CHAT_MESSAGE_CREATED,
                    accessContext.currentUser().getId(),
                    SpaceChatMessage.class.getSimpleName(),
                    UUID.fromString(saved.getId()),
                    "spaceId=" + spaceId
                            + ",messageId=" + saved.getId()
                            + ",authorUserId=" + accessContext.currentUser().getId()
                            + ",bodyLength=" + trimmedBody.length()
            );
            return toResponse(saved);
        } catch (MongoException | DataAccessException ex) {
            throw unavailable(ex);
        }
    }

    private int resolveLimit(Integer requestedLimit) {
        int limit = requestedLimit == null ? spaceChatProperties.getPageSizeDefault() : requestedLimit;
        if (limit < 1 || limit > spaceChatProperties.getPageSizeMax()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Chat page size must be between 1 and " + spaceChatProperties.getPageSizeMax()
            );
        }
        return limit;
    }

    private ChatCursor parseCursor(String beforeCreatedAt, String beforeMessageId) {
        boolean hasCreatedAt = beforeCreatedAt != null && !beforeCreatedAt.isBlank();
        boolean hasMessageId = beforeMessageId != null && !beforeMessageId.isBlank();

        if (hasCreatedAt != hasMessageId) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "beforeCreatedAt and beforeMessageId must be provided together"
            );
        }

        if (!hasCreatedAt) {
            return null;
        }

        try {
            return new ChatCursor(Instant.parse(beforeCreatedAt), UUID.fromString(beforeMessageId));
        } catch (DateTimeParseException | IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chat pagination cursor is invalid");
        }
    }

    private String normalizeBody(String body) {
        return body == null ? "" : body.trim();
    }

    private SpaceChatMessageResponse toResponse(SpaceChatMessage message) {
        return new SpaceChatMessageResponse(
                UUID.fromString(message.getId()),
                UUID.fromString(message.getSpaceId()),
                UUID.fromString(message.getAuthorUserId()),
                message.getAuthorDisplayName(),
                message.getBody(),
                message.getCreatedAt()
        );
    }

    private ResponseStatusException unavailable(Exception ex) {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, CHAT_UNAVAILABLE_MESSAGE, ex);
    }

    private record ChatCursor(Instant createdAt, UUID messageId) {
    }
}

