package edusecure.edusecure.entity.spacechatkey;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "space_chat_key_recipients")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpaceChatKeyRecipient {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID spaceChatKeyVersionId;

    @Column(nullable = false)
    private UUID recipientUserId;

    @Column(nullable = false, length = 64)
    private String wrapAlgorithm;

    @Column(nullable = false, length = 255)
    private String wrapNonce;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String wrappedKeyCiphertext;

    @Column(nullable = false)
    private Instant createdAt;
}

