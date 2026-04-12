package edusecure.edusecure.service.spacechatkey;

import edusecure.edusecure.audit.AuditService;
import edusecure.edusecure.config.chat.SpaceChatProperties;
import edusecure.edusecure.dto.spacechat.e2ee.CurrentUserChatKeyResponse;
import edusecure.edusecure.dto.spacechat.e2ee.PublishSpaceChatKeyVersionRequest;
import edusecure.edusecure.dto.spacechat.e2ee.SpaceChatE2eeRecipientResponse;
import edusecure.edusecure.dto.spacechat.e2ee.SpaceChatE2eeStateResponse;
import edusecure.edusecure.dto.spacechat.e2ee.SpaceChatKeyVersionPublishResponse;
import edusecure.edusecure.dto.spacechat.e2ee.SpaceChatWrappedKeyResponse;
import edusecure.edusecure.dto.spacechat.e2ee.UpsertCurrentUserChatKeyRequest;
import edusecure.edusecure.entity.audit.AuditActionType;
import edusecure.edusecure.entity.auth.User;
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
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SpaceChatE2eeService {

    private static final String E2EE_DISABLED_MESSAGE = "Space chat end-to-end encryption is not enabled";

    private final SpaceAccessService spaceAccessService;
    private final UserRepository userRepository;
    private final SpaceMembershipRepository spaceMembershipRepository;
    private final UserChatKeyRepository userChatKeyRepository;
    private final SpaceChatKeyVersionRepository spaceChatKeyVersionRepository;
    private final SpaceChatKeyRecipientRepository spaceChatKeyRecipientRepository;
    private final SpaceChatProperties spaceChatProperties;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public CurrentUserChatKeyResponse getCurrentUserKey(String currentUserEmail) {
        User currentUser = spaceAccessService.findCurrentUserOrThrow(currentUserEmail);
        return toCurrentUserChatKeyResponse(userChatKeyRepository.findFirstByUserIdAndRevokedAtIsNull(currentUser.getId()));
    }

    @Transactional
    public CurrentUserChatKeyResponse upsertCurrentUserKey(String currentUserEmail, UpsertCurrentUserChatKeyRequest request) {
        requireE2eeEnabled();
        User currentUser = spaceAccessService.findCurrentUserOrThrow(currentUserEmail);
        UserChatKey key = userChatKeyRepository.findFirstByUserIdAndRevokedAtIsNull(currentUser.getId())
                .orElseGet(() -> UserChatKey.builder().userId(currentUser.getId()).build());

        key.setAlgorithm(request.algorithm().trim());
        key.setPublicKeyJwk(request.publicKeyJwk().trim());
        key.setFingerprint(request.fingerprint().trim());
        key.setCreatedAt(Instant.now());
        key.setRevokedAt(null);

        UserChatKey saved = userChatKeyRepository.save(key);
        auditService.record(
                AuditActionType.SPACE_CHAT_KEY_REGISTERED,
                currentUser.getId(),
                UserChatKey.class.getSimpleName(),
                saved.getId(),
                "algorithm=" + saved.getAlgorithm() + ",fingerprint=" + saved.getFingerprint()
        );
        return toCurrentUserChatKeyResponse(Optional.of(saved));
    }

    @Transactional(readOnly = true)
    public List<SpaceChatE2eeRecipientResponse> listRecipients(String currentUserEmail, UUID spaceId) {
        requireE2eeEnabled();
        SpaceAccessContext accessContext = spaceAccessService.requireManageableSpace(currentUserEmail, spaceId);
        List<User> participants = resolveParticipantUsers(accessContext);
        Map<UUID, UserChatKey> keysByUserId = activeKeysByUserId(participants.stream().map(User::getId).toList());

        return participants.stream()
                .map(user -> {
                    UserChatKey key = keysByUserId.get(user.getId());
                    return new SpaceChatE2eeRecipientResponse(
                            user.getId(),
                            user.getFullName(),
                            accessContext.space().getCreatedByUserId().equals(user.getId()),
                            key != null,
                            key != null ? key.getAlgorithm() : null,
                            key != null ? key.getPublicKeyJwk() : null,
                            key != null ? key.getFingerprint() : null
                    );
                })
                .toList();
    }

    @Transactional
    public SpaceChatKeyVersionPublishResponse publishKeyVersion(
            String currentUserEmail,
            UUID spaceId,
            PublishSpaceChatKeyVersionRequest request
    ) {
        requireE2eeEnabled();
        SpaceAccessContext accessContext = spaceAccessService.requireManageableSpace(currentUserEmail, spaceId);

        if (accessContext.space().isArchived()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Archived spaces are read-only");
        }

        List<User> participants = resolveParticipantUsers(accessContext);
        Set<UUID> expectedRecipientIds = participants.stream()
                .map(User::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        UserChatKey publisherKey = userChatKeyRepository.findFirstByUserIdAndRevokedAtIsNull(accessContext.currentUser().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "The current device must register a chat key before publishing a room key"));
        validateWrappedRecipients(request, expectedRecipientIds);

        Map<UUID, UserChatKey> activeKeysByUserId = activeKeysByUserId(expectedRecipientIds);
        if (activeKeysByUserId.size() != expectedRecipientIds.size()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Every current space participant must register a chat key before publishing a room key"
            );
        }

        int expectedKeyVersion = spaceChatKeyVersionRepository.findFirstBySpaceIdOrderByKeyVersionDesc(spaceId)
                .map(existing -> existing.getKeyVersion() + 1)
                .orElse(1);
        if (request.keyVersion() != expectedKeyVersion) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Next chat key version must be " + expectedKeyVersion);
        }

        Instant createdAt = Instant.now();
        SpaceChatKeyVersion savedKeyVersion = spaceChatKeyVersionRepository.save(SpaceChatKeyVersion.builder()
                .spaceId(spaceId)
                .keyVersion(request.keyVersion())
                .createdByUserId(accessContext.currentUser().getId())
                .createdAt(createdAt)
                .rotationReason(request.rotationReason().trim())
                .publisherPublicKeyJwk(publisherKey.getPublicKeyJwk())
                .publisherKeyFingerprint(publisherKey.getFingerprint())
                .requiresRekey(false)
                .build());

        List<SpaceChatKeyRecipient> recipients = request.recipients().stream()
                .map(recipient -> SpaceChatKeyRecipient.builder()
                        .spaceChatKeyVersionId(savedKeyVersion.getId())
                        .recipientUserId(recipient.recipientUserId())
                        .wrapAlgorithm(recipient.wrapAlgorithm().trim())
                        .wrapNonce(recipient.wrapNonce().trim())
                        .wrappedKeyCiphertext(recipient.wrappedKeyCiphertext().trim())
                        .createdAt(createdAt)
                        .build())
                .toList();
        spaceChatKeyRecipientRepository.saveAll(recipients);

        auditService.record(
                AuditActionType.SPACE_CHAT_KEY_VERSION_PUBLISHED,
                accessContext.currentUser().getId(),
                SpaceChatKeyVersion.class.getSimpleName(),
                savedKeyVersion.getId(),
                "spaceId=" + spaceId
                        + ",keyVersion=" + savedKeyVersion.getKeyVersion()
                        + ",recipientCount=" + recipients.size()
                        + ",rotationReason=" + savedKeyVersion.getRotationReason()
        );

        return new SpaceChatKeyVersionPublishResponse(
                savedKeyVersion.getKeyVersion(),
                savedKeyVersion.getRotationReason(),
                recipients.size(),
                savedKeyVersion.isRequiresRekey(),
                savedKeyVersion.getPublisherKeyFingerprint(),
                savedKeyVersion.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public SpaceChatE2eeStateResponse getSpaceState(String currentUserEmail, UUID spaceId) {
        SpaceAccessContext accessContext = spaceAccessService.requireReadableSpace(currentUserEmail, spaceId);
        Optional<UserChatKey> currentUserKey = userChatKeyRepository.findFirstByUserIdAndRevokedAtIsNull(accessContext.currentUser().getId());

        if (!spaceChatProperties.getE2ee().isEnabled()) {
            return new SpaceChatE2eeStateResponse(false, spaceChatProperties.getE2ee().isRequireRegisteredKey(), false, null, false, null);
        }

        Optional<SpaceChatKeyVersion> latestKeyVersion = spaceChatKeyVersionRepository.findFirstBySpaceIdOrderByKeyVersionDesc(spaceId);
        SpaceChatWrappedKeyResponse wrappedKeyResponse = latestKeyVersion
                .flatMap(keyVersion -> currentUserWrappedKey(keyVersion.getId(), accessContext.currentUser().getId())
                        .map(recipient -> toWrappedKeyResponse(keyVersion, recipient)))
                .orElse(null);

        return new SpaceChatE2eeStateResponse(
                true,
                spaceChatProperties.getE2ee().isRequireRegisteredKey(),
                currentUserKey.isPresent(),
                latestKeyVersion.map(SpaceChatKeyVersion::getKeyVersion).orElse(null),
                latestKeyVersion.map(SpaceChatKeyVersion::isRequiresRekey).orElse(true),
                wrappedKeyResponse
        );
    }

    private Optional<SpaceChatKeyRecipient> currentUserWrappedKey(UUID keyVersionId, UUID currentUserId) {
        return spaceChatKeyRecipientRepository.findFirstBySpaceChatKeyVersionIdAndRecipientUserId(keyVersionId, currentUserId);
    }

    private List<User> resolveParticipantUsers(SpaceAccessContext accessContext) {
        LinkedHashSet<UUID> participantIds = new LinkedHashSet<>();
        participantIds.add(accessContext.space().getCreatedByUserId());
        for (SpaceMembership membership : spaceMembershipRepository.findAllBySpaceIdOrderByAddedAtAsc(accessContext.space().getId())) {
            participantIds.add(membership.getStudentUserId());
        }

        Map<UUID, User> usersById = userRepository.findAllById(participantIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));

        List<User> participants = new ArrayList<>();
        for (UUID participantId : participantIds) {
            User participant = usersById.get(participantId);
            if (participant == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found for encrypted chat participant");
            }
            participants.add(participant);
        }
        return participants;
    }

    private Map<UUID, UserChatKey> activeKeysByUserId(Iterable<UUID> userIds) {
        List<UUID> ids = new ArrayList<>();
        for (UUID userId : userIds) {
            ids.add(userId);
        }

        return userChatKeyRepository.findAllByUserIdInAndRevokedAtIsNull(ids).stream()
                .collect(Collectors.toMap(UserChatKey::getUserId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
    }

    private void validateWrappedRecipients(PublishSpaceChatKeyVersionRequest request, Set<UUID> expectedRecipientIds) {
        LinkedHashSet<UUID> actualRecipientIds = new LinkedHashSet<>();
        for (PublishSpaceChatKeyVersionRequest.Recipient recipient : request.recipients()) {
            if (!actualRecipientIds.add(recipient.recipientUserId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Wrapped chat key recipients must not contain duplicates");
            }
        }

        if (!actualRecipientIds.equals(expectedRecipientIds)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Wrapped chat key recipients must match the current space participant set"
            );
        }
    }

    private CurrentUserChatKeyResponse toCurrentUserChatKeyResponse(Optional<UserChatKey> key) {
        return new CurrentUserChatKeyResponse(
                spaceChatProperties.getE2ee().isEnabled(),
                spaceChatProperties.getE2ee().isRequireRegisteredKey(),
                key.isPresent(),
                key.map(UserChatKey::getAlgorithm).orElse(null),
                key.map(UserChatKey::getPublicKeyJwk).orElse(null),
                key.map(UserChatKey::getFingerprint).orElse(null),
                key.map(UserChatKey::getCreatedAt).orElse(null)
        );
    }

    private SpaceChatWrappedKeyResponse toWrappedKeyResponse(SpaceChatKeyVersion keyVersion, SpaceChatKeyRecipient recipient) {
        return new SpaceChatWrappedKeyResponse(
                keyVersion.getCreatedByUserId(),
                keyVersion.getPublisherPublicKeyJwk(),
                keyVersion.getPublisherKeyFingerprint(),
                recipient.getWrapAlgorithm(),
                recipient.getWrapNonce(),
                recipient.getWrappedKeyCiphertext(),
                recipient.getCreatedAt()
        );
    }

    private void requireE2eeEnabled() {
        if (!spaceChatProperties.getE2ee().isEnabled()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, E2EE_DISABLED_MESSAGE);
        }
    }
}

