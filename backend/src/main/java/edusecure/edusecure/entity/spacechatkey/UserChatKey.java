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
@Table(name = "user_chat_keys")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserChatKey {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 32)
    private String algorithm;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String publicKeyJwk;

    @Column(nullable = false, length = 128)
    private String fingerprint;

    @Column(nullable = false)
    private Instant createdAt;

    @Column
    private Instant revokedAt;
}

