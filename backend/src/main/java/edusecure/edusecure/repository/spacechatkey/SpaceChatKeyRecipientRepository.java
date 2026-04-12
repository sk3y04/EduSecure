package edusecure.edusecure.repository.spacechatkey;

import edusecure.edusecure.entity.spacechatkey.SpaceChatKeyRecipient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpaceChatKeyRecipientRepository extends JpaRepository<SpaceChatKeyRecipient, UUID> {

    Optional<SpaceChatKeyRecipient> findFirstBySpaceChatKeyVersionIdAndRecipientUserId(UUID spaceChatKeyVersionId, UUID recipientUserId);
}

