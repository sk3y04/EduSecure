package edusecure.edusecure.service.spacechatkey;

import edusecure.edusecure.config.chat.SpaceChatProperties;
import edusecure.edusecure.entity.spacechatkey.SpaceChatKeyVersion;
import edusecure.edusecure.repository.spacechatkey.SpaceChatKeyVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SpaceChatKeyVersionService {

    private final SpaceChatKeyVersionRepository spaceChatKeyVersionRepository;
    private final SpaceChatProperties spaceChatProperties;

    @Transactional(readOnly = true)
    public Optional<SpaceChatKeyVersion> findLatestForSpace(UUID spaceId) {
        return spaceChatKeyVersionRepository.findFirstBySpaceIdOrderByKeyVersionDesc(spaceId);
    }

    @Transactional
    public void markSpaceRequiresRekey(UUID spaceId) {
        if (!spaceChatProperties.getE2ee().isEnabled()) {
            return;
        }

        spaceChatKeyVersionRepository.findFirstBySpaceIdOrderByKeyVersionDesc(spaceId)
                .filter(keyVersion -> !keyVersion.isRequiresRekey())
                .ifPresent(keyVersion -> {
                    keyVersion.setRequiresRekey(true);
                    spaceChatKeyVersionRepository.save(keyVersion);
                });
    }
}

