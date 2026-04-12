package edusecure.edusecure;

import edusecure.edusecure.audit.AuditService;
import edusecure.edusecure.config.chat.SpaceChatProperties;
import edusecure.edusecure.dto.spacechat.e2ee.CurrentUserChatKeyResponse;
import edusecure.edusecure.dto.spacechat.e2ee.PublishSpaceChatKeyVersionRequest;
import edusecure.edusecure.dto.spacechat.e2ee.SpaceChatE2eeRecipientResponse;
import edusecure.edusecure.dto.spacechat.e2ee.SpaceChatE2eeStateResponse;
import edusecure.edusecure.dto.spacechat.e2ee.SpaceChatKeyVersionPublishResponse;
import edusecure.edusecure.dto.spacechat.e2ee.UpsertCurrentUserChatKeyRequest;
import edusecure.edusecure.entity.auth.User;
import edusecure.edusecure.entity.space.Space;
import edusecure.edusecure.entity.space.SpaceMembership;
import edusecure.edusecure.entity.spacechatkey.SpaceChatKeyRecipient;
import edusecure.edusecure.entity.spacechatkey.SpaceChatKeyVersion;
import edusecure.edusecure.entity.spacechatkey.UserChatKey;
import edusecure.edusecure.repository.auth.UserRepository;
import edusecure.edusecure.repository.space.SpaceMembershipRepository;
import edusecure.edusecure.repository.spacechatkey.SpaceChatKeyRecipientRepository;
import edusecure.edusecure.repository.spacechatkey.SpaceChatKeyVersionRepository;
import edusecure.edusecure.repository.spacechatkey.UserChatKeyRepository;
import edusecure.edusecure.service.space.SpaceAccessContext;
import edusecure.edusecure.service.space.SpaceAccessService;
import edusecure.edusecure.service.spacechatkey.SpaceChatE2eeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
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
class SpaceChatE2eeServiceTests {

    @Mock
    private SpaceAccessService spaceAccessService;

    @Mock
    private UserChatKeyRepository userChatKeyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SpaceMembershipRepository spaceMembershipRepository;

    @Mock
    private SpaceChatKeyVersionRepository spaceChatKeyVersionRepository;

    @Mock
    private SpaceChatKeyRecipientRepository spaceChatKeyRecipientRepository;

    @Mock
    private AuditService auditService;

    private SpaceChatProperties spaceChatProperties;

    private SpaceChatE2eeService spaceChatE2eeService;

    @BeforeEach
    void setUp() {
        spaceChatProperties = new SpaceChatProperties();
        spaceChatProperties.setEnabled(true);
        spaceChatProperties.getE2ee().setEnabled(true);
        spaceChatProperties.getE2ee().setRequireRegisteredKey(false);
        spaceChatE2eeService = new SpaceChatE2eeService(
                spaceAccessService,
                userRepository,
                spaceMembershipRepository,
                userChatKeyRepository,
                spaceChatKeyVersionRepository,
                spaceChatKeyRecipientRepository,
                spaceChatProperties,
                auditService
        );
    }

    @Test
    void getCurrentUserKeyReturnsMissingStateWhenNoKeyIsRegistered() {
        UUID userId = UUID.randomUUID();
        when(spaceAccessService.findCurrentUserOrThrow("student@example.com"))
                .thenReturn(user(userId, "student@example.com", "Student Example"));
        when(userChatKeyRepository.findFirstByUserIdAndRevokedAtIsNull(userId)).thenReturn(Optional.empty());

        CurrentUserChatKeyResponse response = spaceChatE2eeService.getCurrentUserKey("student@example.com");

        assertThat(response.e2eeEnabled()).isTrue();
        assertThat(response.keyRegistered()).isFalse();
        assertThat(response.algorithm()).isNull();
        assertThat(response.publicKeyJwk()).isNull();
    }

    @Test
    void upsertCurrentUserKeyPersistsAndAuditsRegistration() {
        UUID userId = UUID.randomUUID();
        when(spaceAccessService.findCurrentUserOrThrow("student@example.com"))
                .thenReturn(user(userId, "student@example.com", "Student Example"));
        when(userChatKeyRepository.findFirstByUserIdAndRevokedAtIsNull(userId)).thenReturn(Optional.empty());
        when(userChatKeyRepository.save(any(UserChatKey.class))).thenAnswer(invocation -> {
            UserChatKey key = invocation.getArgument(0);
            if (key.getId() == null) {
                key.setId(UUID.randomUUID());
            }
            return key;
        });

        CurrentUserChatKeyResponse response = spaceChatE2eeService.upsertCurrentUserKey(
                "student@example.com",
                new UpsertCurrentUserChatKeyRequest("ECDH_P256", "{\"kty\":\"EC\"}", "fingerprint-123")
        );

        assertThat(response.e2eeEnabled()).isTrue();
        assertThat(response.keyRegistered()).isTrue();
        assertThat(response.algorithm()).isEqualTo("ECDH_P256");
        assertThat(response.fingerprint()).isEqualTo("fingerprint-123");

        ArgumentCaptor<UserChatKey> captor = ArgumentCaptor.forClass(UserChatKey.class);
        verify(userChatKeyRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(userId);
        assertThat(captor.getValue().getPublicKeyJwk()).isEqualTo("{\"kty\":\"EC\"}");
        assertThat(captor.getValue().getCreatedAt()).isNotNull();

        verify(auditService).record(any(), eq(userId), eq(UserChatKey.class.getSimpleName()), any(), eq("algorithm=ECDH_P256,fingerprint=fingerprint-123"));
    }

    @Test
    void upsertCurrentUserKeyRejectsWhenE2eeIsDisabled() {
        spaceChatProperties.getE2ee().setEnabled(false);

        assertThatThrownBy(() -> spaceChatE2eeService.upsertCurrentUserKey(
                "student@example.com",
                new UpsertCurrentUserChatKeyRequest("ECDH_P256", "{}", "fp")
        ))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE))
                .hasMessageContaining("Space chat end-to-end encryption is not enabled");

        verify(userChatKeyRepository, never()).save(any());
    }

    @Test
    void getSpaceStateReturnsLatestVersionAndWrappedKeyForCurrentUser() {
        UUID userId = UUID.randomUUID();
        UUID spaceId = UUID.randomUUID();
        UUID keyVersionId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-04-12T10:15:30Z");

        when(spaceAccessService.requireReadableSpace("student@example.com", spaceId))
                .thenReturn(readableContext(userId, spaceId));
        when(userChatKeyRepository.findFirstByUserIdAndRevokedAtIsNull(userId))
                .thenReturn(Optional.of(UserChatKey.builder()
                        .id(UUID.randomUUID())
                        .userId(userId)
                        .algorithm("ECDH_P256")
                        .publicKeyJwk("{}")
                        .fingerprint("fp")
                        .createdAt(createdAt)
                        .build()));
        when(spaceChatKeyVersionRepository.findFirstBySpaceIdOrderByKeyVersionDesc(spaceId))
                .thenReturn(Optional.of(SpaceChatKeyVersion.builder()
                        .id(keyVersionId)
                        .spaceId(spaceId)
                        .keyVersion(2)
                        .createdByUserId(userId)
                        .createdAt(createdAt)
                        .rotationReason("INITIAL_SETUP")
                        .publisherPublicKeyJwk("publisher-key")
                        .publisherKeyFingerprint("publisher-fp")
                        .requiresRekey(false)
                        .build()));
        when(spaceChatKeyRecipientRepository.findFirstBySpaceChatKeyVersionIdAndRecipientUserId(keyVersionId, userId))
                .thenReturn(Optional.of(SpaceChatKeyRecipient.builder()
                        .id(UUID.randomUUID())
                        .spaceChatKeyVersionId(keyVersionId)
                        .recipientUserId(userId)
                        .wrapAlgorithm("ECDH_P256_HKDF_SHA256_AES_GCM")
                        .wrapNonce("nonce-1")
                        .wrappedKeyCiphertext("ciphertext-1")
                        .createdAt(createdAt)
                        .build()));

        SpaceChatE2eeStateResponse response = spaceChatE2eeService.getSpaceState("student@example.com", spaceId);

        assertThat(response.e2eeEnabled()).isTrue();
        assertThat(response.currentUserKeyRegistered()).isTrue();
        assertThat(response.activeKeyVersion()).isEqualTo(2);
        assertThat(response.requiresRekey()).isFalse();
        assertThat(response.currentUserWrappedKey()).isNotNull();
        assertThat(response.currentUserWrappedKey().publisherUserId()).isEqualTo(userId);
        assertThat(response.currentUserWrappedKey().publisherPublicKeyJwk()).isEqualTo("publisher-key");
        assertThat(response.currentUserWrappedKey().wrapAlgorithm()).isEqualTo("ECDH_P256_HKDF_SHA256_AES_GCM");
        assertThat(response.currentUserWrappedKey().wrappedKeyCiphertext()).isEqualTo("ciphertext-1");
    }

    @Test
    void listRecipientsReturnsCreatorAndMembersWithKeyRegistrationState() {
        UUID creatorId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID spaceId = UUID.randomUUID();

        when(spaceAccessService.requireManageableSpace("lecturer@example.com", spaceId))
                .thenReturn(manageableContext(creatorId, spaceId));
        when(spaceMembershipRepository.findAllBySpaceIdOrderByAddedAtAsc(spaceId))
                .thenReturn(List.of(SpaceMembership.builder()
                        .id(UUID.randomUUID())
                        .spaceId(spaceId)
                        .studentUserId(studentId)
                        .addedByUserId(creatorId)
                        .addedAt(Instant.parse("2026-04-12T09:00:00Z"))
                        .build()));
        when(userRepository.findAllById(any(Iterable.class)))
                .thenReturn(List.of(
                        user(creatorId, "lecturer@example.com", "Lecturer Example"),
                        user(studentId, "student@example.com", "Student Example")
                ));
        when(userChatKeyRepository.findAllByUserIdInAndRevokedAtIsNull(any()))
                .thenReturn(List.of(UserChatKey.builder()
                        .id(UUID.randomUUID())
                        .userId(creatorId)
                        .algorithm("ECDH_P256")
                        .publicKeyJwk("creator-key")
                        .fingerprint("creator-fp")
                        .createdAt(Instant.now())
                        .build()));

        List<SpaceChatE2eeRecipientResponse> recipients = spaceChatE2eeService.listRecipients("lecturer@example.com", spaceId);

        assertThat(recipients).hasSize(2);
        assertThat(recipients.get(0).userId()).isEqualTo(creatorId);
        assertThat(recipients.get(0).manager()).isTrue();
        assertThat(recipients.get(0).keyRegistered()).isTrue();
        assertThat(recipients.get(1).userId()).isEqualTo(studentId);
        assertThat(recipients.get(1).manager()).isFalse();
        assertThat(recipients.get(1).keyRegistered()).isFalse();
    }

    @Test
    void publishKeyVersionPersistsWrappedKeysAndAuditsMetadataOnly() {
        UUID creatorId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID spaceId = UUID.randomUUID();

        when(spaceAccessService.requireManageableSpace("lecturer@example.com", spaceId))
                .thenReturn(manageableContext(creatorId, spaceId));
        when(spaceMembershipRepository.findAllBySpaceIdOrderByAddedAtAsc(spaceId))
                .thenReturn(List.of(SpaceMembership.builder()
                        .id(UUID.randomUUID())
                        .spaceId(spaceId)
                        .studentUserId(studentId)
                        .addedByUserId(creatorId)
                        .addedAt(Instant.parse("2026-04-12T09:00:00Z"))
                        .build()));
        when(userRepository.findAllById(any(Iterable.class)))
                .thenReturn(List.of(
                        user(creatorId, "lecturer@example.com", "Lecturer Example"),
                        user(studentId, "student@example.com", "Student Example")
                ));
        when(userChatKeyRepository.findFirstByUserIdAndRevokedAtIsNull(creatorId))
                .thenReturn(Optional.of(UserChatKey.builder()
                        .id(UUID.randomUUID())
                        .userId(creatorId)
                        .algorithm("ECDH_P256")
                        .publicKeyJwk("creator-key")
                        .fingerprint("creator-fp")
                        .createdAt(Instant.now())
                        .build()));
        when(userChatKeyRepository.findAllByUserIdInAndRevokedAtIsNull(any()))
                .thenReturn(List.of(
                        UserChatKey.builder().id(UUID.randomUUID()).userId(creatorId).algorithm("ECDH_P256").publicKeyJwk("creator-key").fingerprint("creator-fp").createdAt(Instant.now()).build(),
                        UserChatKey.builder().id(UUID.randomUUID()).userId(studentId).algorithm("ECDH_P256").publicKeyJwk("student-key").fingerprint("student-fp").createdAt(Instant.now()).build()
                ));
        when(spaceChatKeyVersionRepository.findFirstBySpaceIdOrderByKeyVersionDesc(spaceId)).thenReturn(Optional.empty());
        when(spaceChatKeyVersionRepository.save(any(SpaceChatKeyVersion.class))).thenAnswer(invocation -> {
            SpaceChatKeyVersion version = invocation.getArgument(0);
            if (version.getId() == null) {
                version.setId(UUID.randomUUID());
            }
            return version;
        });

        SpaceChatKeyVersionPublishResponse response = spaceChatE2eeService.publishKeyVersion(
                "lecturer@example.com",
                spaceId,
                new PublishSpaceChatKeyVersionRequest(
                        1,
                        "INITIAL_SETUP",
                        List.of(
                                new PublishSpaceChatKeyVersionRequest.Recipient(creatorId, "ECDH_P256_HKDF_SHA256_AES_GCM", "nonce-1", "ciphertext-1"),
                                new PublishSpaceChatKeyVersionRequest.Recipient(studentId, "ECDH_P256_HKDF_SHA256_AES_GCM", "nonce-2", "ciphertext-2")
                        )
                )
        );

        assertThat(response.keyVersion()).isEqualTo(1);
        assertThat(response.rotationReason()).isEqualTo("INITIAL_SETUP");
        assertThat(response.recipientCount()).isEqualTo(2);
        assertThat(response.requiresRekey()).isFalse();
        assertThat(response.publisherKeyFingerprint()).isEqualTo("creator-fp");

        ArgumentCaptor<SpaceChatKeyVersion> versionCaptor = ArgumentCaptor.forClass(SpaceChatKeyVersion.class);
        verify(spaceChatKeyVersionRepository).save(versionCaptor.capture());
        assertThat(versionCaptor.getValue().getSpaceId()).isEqualTo(spaceId);
        assertThat(versionCaptor.getValue().getCreatedByUserId()).isEqualTo(creatorId);

        ArgumentCaptor<List<SpaceChatKeyRecipient>> recipientsCaptor = ArgumentCaptor.forClass(List.class);
        verify(spaceChatKeyRecipientRepository).saveAll(recipientsCaptor.capture());
        assertThat(recipientsCaptor.getValue()).hasSize(2);
        assertThat(recipientsCaptor.getValue()).extracting(SpaceChatKeyRecipient::getRecipientUserId)
                .containsExactly(creatorId, studentId);

        verify(auditService).record(any(), eq(creatorId), eq(SpaceChatKeyVersion.class.getSimpleName()), any(), eq("spaceId=" + spaceId + ",keyVersion=1,recipientCount=2,rotationReason=INITIAL_SETUP"));
    }

    @Test
    void publishKeyVersionRejectsRecipientMismatch() {
        UUID creatorId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID unexpectedId = UUID.randomUUID();
        UUID spaceId = UUID.randomUUID();

        when(spaceAccessService.requireManageableSpace("lecturer@example.com", spaceId))
                .thenReturn(manageableContext(creatorId, spaceId));
        when(spaceMembershipRepository.findAllBySpaceIdOrderByAddedAtAsc(spaceId))
                .thenReturn(List.of(SpaceMembership.builder()
                        .id(UUID.randomUUID())
                        .spaceId(spaceId)
                        .studentUserId(studentId)
                        .addedByUserId(creatorId)
                        .addedAt(Instant.parse("2026-04-12T09:00:00Z"))
                        .build()));
        when(userRepository.findAllById(any(Iterable.class)))
                .thenReturn(List.of(
                        user(creatorId, "lecturer@example.com", "Lecturer Example"),
                        user(studentId, "student@example.com", "Student Example")
                ));
        when(userChatKeyRepository.findFirstByUserIdAndRevokedAtIsNull(creatorId))
                .thenReturn(Optional.of(UserChatKey.builder()
                        .id(UUID.randomUUID())
                        .userId(creatorId)
                        .algorithm("ECDH_P256")
                        .publicKeyJwk("creator-key")
                        .fingerprint("creator-fp")
                        .createdAt(Instant.now())
                        .build()));

        assertThatThrownBy(() -> spaceChatE2eeService.publishKeyVersion(
                "lecturer@example.com",
                spaceId,
                new PublishSpaceChatKeyVersionRequest(
                        1,
                        "INITIAL_SETUP",
                        List.of(
                                new PublishSpaceChatKeyVersionRequest.Recipient(creatorId, "ECDH_P256_HKDF_SHA256_AES_GCM", "nonce-1", "ciphertext-1"),
                                new PublishSpaceChatKeyVersionRequest.Recipient(unexpectedId, "ECDH_P256_HKDF_SHA256_AES_GCM", "nonce-x", "ciphertext-x")
                        )
                )
        ))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST))
                .hasMessageContaining("Wrapped chat key recipients must match the current space participant set");

        verify(spaceChatKeyVersionRepository, never()).save(any());
        verify(spaceChatKeyRecipientRepository, never()).saveAll(any());
    }

    private static User user(UUID userId, String email, String fullName) {
        return User.builder()
                .id(userId)
                .email(email)
                .fullName(fullName)
                .roles(Set.of())
                .build();
    }

    private static SpaceAccessContext readableContext(UUID userId, UUID spaceId) {
        return new SpaceAccessContext(
                user(userId, "student@example.com", "Student Example"),
                Space.builder().id(spaceId).archived(false).build(),
                false,
                true
        );
    }

    private static SpaceAccessContext manageableContext(UUID userId, UUID spaceId) {
        return new SpaceAccessContext(
                user(userId, "lecturer@example.com", "Lecturer Example"),
                Space.builder().id(spaceId).createdByUserId(userId).archived(false).build(),
                true,
                false
        );
    }
}

