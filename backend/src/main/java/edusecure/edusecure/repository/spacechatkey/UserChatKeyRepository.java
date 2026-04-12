package edusecure.edusecure.repository.spacechatkey;

import edusecure.edusecure.entity.spacechatkey.UserChatKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserChatKeyRepository extends JpaRepository<UserChatKey, UUID> {

    Optional<UserChatKey> findFirstByUserIdAndRevokedAtIsNull(UUID userId);

    List<UserChatKey> findAllByUserIdInAndRevokedAtIsNull(Collection<UUID> userIds);
}

