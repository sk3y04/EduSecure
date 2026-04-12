package edusecure.edusecure.repository.spacechat;

import edusecure.edusecure.document.spacechat.SpaceChatMessage;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SpaceChatMessageRepository extends MongoRepository<SpaceChatMessage, String> {
}

