package edusecure.edusecure;

import edusecure.edusecure.audit.AuditService;
import edusecure.edusecure.config.chat.SpaceChatProperties;
import edusecure.edusecure.document.spacechat.SpaceChatMessage;
import edusecure.edusecure.dto.spacechat.CreateSpaceChatMessageRequest;
import edusecure.edusecure.dto.spacechat.SpaceChatMessagePageResponse;
import edusecure.edusecure.dto.spacechat.SpaceChatMessageResponse;
import edusecure.edusecure.entity.audit.AuditActionType;
import edusecure.edusecure.entity.auth.User;
import edusecure.edusecure.entity.space.Space;
import edusecure.edusecure.repository.spacechat.SpaceChatMessageRepository;
import edusecure.edusecure.service.space.SpaceAccessContext;
import edusecure.edusecure.service.space.SpaceAccessService;
import edusecure.edusecure.service.spacechat.DisabledSpaceChatService;
import edusecure.edusecure.service.spacechat.MongoSpaceChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpaceChatIntegrationTests {

    @Mock
    private SpaceAccessService spaceAccessService;

    @Mock
    private SpaceChatMessageRepository spaceChatMessageRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private AuditService auditService;

    private SpaceChatProperties spaceChatProperties;

    private MongoSpaceChatService mongoSpaceChatService;

    private DisabledSpaceChatService disabledSpaceChatService;

    @BeforeEach
    void setUp() {
        spaceChatProperties = new SpaceChatProperties();
        spaceChatProperties.setEnabled(true);
        spaceChatProperties.setMessageMaxLength(2000);
        spaceChatProperties.setPageSizeDefault(30);
        spaceChatProperties.setPageSizeMax(100);
        mongoSpaceChatService = new MongoSpaceChatService(
                spaceAccessService,
                spaceChatMessageRepository,
                mongoTemplate,
                auditService,
                spaceChatProperties
        );
        disabledSpaceChatService = new DisabledSpaceChatService();
    }

    @Test
    void listMessagesReturnsOldestToNewestPageAndDeterministicCursor() {
        UUID spaceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        SpaceAccessContext accessContext = readableContext(userId, spaceId, false, true, false);
        Instant sameTimestamp = Instant.parse("2026-04-11T12:34:56.789Z");

        SpaceChatMessage first = message("00000000-0000-0000-0000-000000000003", spaceId, userId, "Student One", "third", sameTimestamp);
        SpaceChatMessage second = message("00000000-0000-0000-0000-000000000002", spaceId, userId, "Student One", "second", sameTimestamp);
        SpaceChatMessage overflow = message("00000000-0000-0000-0000-000000000001", spaceId, userId, "Student One", "first", sameTimestamp);

        when(spaceAccessService.requireReadableSpace("student@example.com", spaceId)).thenReturn(accessContext);
        when(mongoTemplate.find(any(Query.class), eq(SpaceChatMessage.class))).thenReturn(List.of(first, second, overflow));

        SpaceChatMessagePageResponse response = mongoSpaceChatService.listMessages(
                "student@example.com",
                spaceId,
                2,
                null,
                null
        );

        assertThat(response.items()).extracting(SpaceChatMessageResponse::body).containsExactly("second", "third");
        assertThat(response.hasMore()).isTrue();
        assertThat(response.nextCursorBeforeCreatedAt()).isEqualTo(sameTimestamp);
        assertThat(response.nextCursorBeforeMessageId()).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000002"));

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).find(queryCaptor.capture(), eq(SpaceChatMessage.class));
        Query query = queryCaptor.getValue();
        assertThat(query.getLimit()).isEqualTo(3);
        assertThat(query.getQueryObject().toJson()).contains(spaceId.toString());
        assertThat(query.getSortObject().toJson()).contains("createdAt").contains("_id");
    }

    @Test
    void listMessagesIncludesCursorCriteriaWhenProvided() {
        UUID spaceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant cursorTime = Instant.parse("2026-04-11T12:34:56.789Z");

        when(spaceAccessService.requireReadableSpace("student@example.com", spaceId))
                .thenReturn(readableContext(userId, spaceId, false, true, false));
        when(mongoTemplate.find(any(Query.class), eq(SpaceChatMessage.class))).thenReturn(List.of());

        SpaceChatMessagePageResponse response = mongoSpaceChatService.listMessages(
                "student@example.com",
                spaceId,
                30,
                cursorTime.toString(),
                "00000000-0000-0000-0000-000000000010"
        );

        assertThat(response.items()).isEmpty();
        assertThat(response.hasMore()).isFalse();

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).find(queryCaptor.capture(), eq(SpaceChatMessage.class));
        String queryJson = String.valueOf(queryCaptor.getValue().getQueryObject());
        assertThat(queryJson)
                .contains("$or")
                .contains("createdAt")
                .contains("_id")
                .contains("00000000-0000-0000-0000-000000000010");
    }

    @Test
    void listMessagesRejectsInvalidPaginationArguments() {
        UUID spaceId = UUID.randomUUID();

        when(spaceAccessService.requireReadableSpace("student@example.com", spaceId))
                .thenReturn(readableContext(UUID.randomUUID(), spaceId, false, true, false));

        assertThatThrownBy(() -> mongoSpaceChatService.listMessages("student@example.com", spaceId, 0, null, null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST))
                .hasMessageContaining("Chat page size must be between 1 and 100");

        assertThatThrownBy(() -> mongoSpaceChatService.listMessages("student@example.com", spaceId, 10, Instant.now().toString(), null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST))
                .hasMessageContaining("beforeCreatedAt and beforeMessageId must be provided together");

        assertThatThrownBy(() -> mongoSpaceChatService.listMessages("student@example.com", spaceId, 10, "bad", "bad"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST))
                .hasMessageContaining("Chat pagination cursor is invalid");
    }

    @Test
    void createMessageTrimsPersistsAndAuditsMetadataOnly() {
        UUID spaceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        User currentUser = User.builder()
                .id(userId)
                .email("student@example.com")
                .fullName("Student Example")
                .roles(Set.of())
                .build();
        Space space = Space.builder().id(spaceId).archived(false).build();
        SpaceAccessContext accessContext = new SpaceAccessContext(currentUser, space, false, true);

        when(spaceAccessService.requireWritableChatSpace("student@example.com", spaceId)).thenReturn(accessContext);
        when(spaceChatMessageRepository.save(any(SpaceChatMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SpaceChatMessageResponse response = mongoSpaceChatService.createMessage(
                "student@example.com",
                spaceId,
                new CreateSpaceChatMessageRequest("  Can someone confirm the deadline?  ")
        );

        assertThat(response.spaceId()).isEqualTo(spaceId);
        assertThat(response.authorUserId()).isEqualTo(userId);
        assertThat(response.authorDisplayName()).isEqualTo("Student Example");
        assertThat(response.body()).isEqualTo("Can someone confirm the deadline?");
        assertThat(response.id()).isNotNull();
        assertThat(response.createdAt()).isNotNull();

        ArgumentCaptor<SpaceChatMessage> messageCaptor = ArgumentCaptor.forClass(SpaceChatMessage.class);
        verify(spaceChatMessageRepository).save(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getBody()).isEqualTo("Can someone confirm the deadline?");
        assertThat(messageCaptor.getValue().getAuthorDisplayName()).isEqualTo("Student Example");

        ArgumentCaptor<String> auditDetailsCaptor = ArgumentCaptor.forClass(String.class);
        verify(auditService).record(
                eq(AuditActionType.SPACE_CHAT_MESSAGE_CREATED),
                eq(userId),
                eq(SpaceChatMessage.class.getSimpleName()),
                eq(response.id()),
                auditDetailsCaptor.capture()
        );
        assertThat(auditDetailsCaptor.getValue())
                .contains("spaceId=" + spaceId)
                .contains("messageId=" + response.id())
                .contains("authorUserId=" + userId)
                .contains("bodyLength=33")
                .doesNotContain("Can someone confirm the deadline?")
                .doesNotContain("student@example.com");
    }

    @Test
    void createMessageRejectsBlankAndTooLongBodies() {
        UUID spaceId = UUID.randomUUID();
        when(spaceAccessService.requireWritableChatSpace("student@example.com", spaceId))
                .thenReturn(readableContext(UUID.randomUUID(), spaceId, false, true, false));

        assertThatThrownBy(() -> mongoSpaceChatService.createMessage(
                "student@example.com",
                spaceId,
                new CreateSpaceChatMessageRequest("   ")
        ))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST))
                .hasMessageContaining("Message body is required");

        assertThatThrownBy(() -> mongoSpaceChatService.createMessage(
                "student@example.com",
                spaceId,
                new CreateSpaceChatMessageRequest("x".repeat(2001))
        ))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST))
                .hasMessageContaining("Message body must not exceed 2000 characters");

        verify(spaceChatMessageRepository, never()).save(any());
        verify(auditService, never()).record(any(), any(), any(), any(), any());
    }

    @Test
    void repositoryFailuresBecomeServiceUnavailable() {
        UUID spaceId = UUID.randomUUID();
        when(spaceAccessService.requireReadableSpace("student@example.com", spaceId))
                .thenReturn(readableContext(UUID.randomUUID(), spaceId, false, true, false));
        when(mongoTemplate.find(any(Query.class), eq(SpaceChatMessage.class)))
                .thenThrow(new DataAccessResourceFailureException("mongo down"));

        assertThatThrownBy(() -> mongoSpaceChatService.listMessages("student@example.com", spaceId, null, null, null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE))
                .hasMessageContaining("Space chat is temporarily unavailable");
    }

    @Test
    void disabledChatServiceAlwaysReturnsServiceUnavailable() {
        assertThatThrownBy(() -> disabledSpaceChatService.listMessages("user@example.com", UUID.randomUUID(), null, null, null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE))
                .hasMessageContaining("Space chat is temporarily unavailable");

        assertThatThrownBy(() -> disabledSpaceChatService.createMessage(
                "user@example.com",
                UUID.randomUUID(),
                new CreateSpaceChatMessageRequest("hello")
        ))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE))
                .hasMessageContaining("Space chat is temporarily unavailable");
    }

    private static SpaceAccessContext readableContext(UUID userId, UUID spaceId, boolean canManage, boolean isMember, boolean archived) {
        User currentUser = User.builder()
                .id(userId)
                .email("user@example.com")
                .fullName("Example User")
                .roles(Set.of())
                .build();
        Space space = Space.builder()
                .id(spaceId)
                .archived(archived)
                .build();
        return new SpaceAccessContext(currentUser, space, canManage, isMember);
    }

    private static SpaceChatMessage message(
            String id,
            UUID spaceId,
            UUID authorUserId,
            String authorDisplayName,
            String body,
            Instant createdAt
    ) {
        return SpaceChatMessage.builder()
                .id(id)
                .spaceId(spaceId.toString())
                .authorUserId(authorUserId.toString())
                .authorDisplayName(authorDisplayName)
                .body(body)
                .createdAt(createdAt)
                .build();
    }
}






