package edusecure.edusecure.document.spacechat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "space_chat_messages")
@CompoundIndex(name = "space_created_id_desc_idx", def = "{'spaceId': 1, 'createdAt': -1, '_id': -1}")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpaceChatMessage {

    @Id
    private String id;

    private String spaceId;

    private String authorUserId;

    private String authorDisplayName;

    private String body;

    private Integer keyVersion;

    private String algorithm;

    private String nonce;

    private String ciphertext;

    private String contentType;

    private Integer plaintextLength;

    private Instant createdAt;
}

