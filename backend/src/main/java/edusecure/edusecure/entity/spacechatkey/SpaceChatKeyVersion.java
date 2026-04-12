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
@Table(name = "space_chat_key_versions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpaceChatKeyVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID spaceId;

    @Column(nullable = false)
    private int keyVersion;

    @Column(nullable = false)
    private UUID createdByUserId;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false, length = 64)
    private String rotationReason;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String publisherPublicKeyJwk;

    @Column(nullable = false, length = 128)
    private String publisherKeyFingerprint;

    @Column(nullable = false)
    private boolean requiresRekey;
}

