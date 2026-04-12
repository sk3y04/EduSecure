package edusecure.edusecure.repository.spacechatkey;

import edusecure.edusecure.entity.spacechatkey.SpaceChatKeyVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpaceChatKeyVersionRepository extends JpaRepository<SpaceChatKeyVersion, UUID> {

    Optional<SpaceChatKeyVersion> findFirstBySpaceIdOrderByKeyVersionDesc(UUID spaceId);
}

