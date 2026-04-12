package edusecure.edusecure;

import edusecure.edusecure.config.chat.SpaceChatProperties;
import edusecure.edusecure.entity.spacechatkey.SpaceChatKeyVersion;
import edusecure.edusecure.repository.spacechatkey.SpaceChatKeyVersionRepository;
import edusecure.edusecure.service.spacechatkey.SpaceChatKeyVersionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpaceChatKeyVersionServiceTests {

    @Mock
    private SpaceChatKeyVersionRepository spaceChatKeyVersionRepository;

    private SpaceChatProperties spaceChatProperties;

    private SpaceChatKeyVersionService spaceChatKeyVersionService;

    @BeforeEach
    void setUp() {
        spaceChatProperties = new SpaceChatProperties();
        spaceChatProperties.getE2ee().setEnabled(true);
        spaceChatKeyVersionService = new SpaceChatKeyVersionService(spaceChatKeyVersionRepository, spaceChatProperties);
    }

    @Test
    void markSpaceRequiresRekeyUpdatesLatestKeyVersionWhenPresent() {
        UUID spaceId = UUID.randomUUID();
        SpaceChatKeyVersion latest = SpaceChatKeyVersion.builder()
                .id(UUID.randomUUID())
                .spaceId(spaceId)
                .keyVersion(1)
                .createdByUserId(UUID.randomUUID())
                .createdAt(Instant.now())
                .rotationReason("INITIAL_SETUP")
                .publisherPublicKeyJwk("{}")
                .publisherKeyFingerprint("fp")
                .requiresRekey(false)
                .build();
        when(spaceChatKeyVersionRepository.findFirstBySpaceIdOrderByKeyVersionDesc(spaceId)).thenReturn(Optional.of(latest));

        spaceChatKeyVersionService.markSpaceRequiresRekey(spaceId);

        assertThat(latest.isRequiresRekey()).isTrue();
        verify(spaceChatKeyVersionRepository).save(latest);
    }

    @Test
    void markSpaceRequiresRekeyDoesNothingWhenE2eeDisabled() {
        UUID spaceId = UUID.randomUUID();
        spaceChatProperties.getE2ee().setEnabled(false);

        spaceChatKeyVersionService.markSpaceRequiresRekey(spaceId);

        verify(spaceChatKeyVersionRepository, never()).findFirstBySpaceIdOrderByKeyVersionDesc(any());
        verify(spaceChatKeyVersionRepository, never()).save(any());
    }
}

