package edusecure.edusecure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "submissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID assignmentId;

    @Column(nullable = false)
    private UUID studentUserId;

    @Column(nullable = false)
    private Instant submittedAt;

    @Column(nullable = false, length = 255)
    private String fileName;

    @Column(nullable = false, length = 255)
    private String contentType;

    @Column(nullable = false, length = 255)
    private String storedFileReference;

    @Column(length = 64)
    private String storageEncryptionAlgorithm;

    @Column(length = 255)
    private String storageEncryptionNonce;

    @Column(length = 512)
    private String wrappedContentEncryptionKey;

    @Column(length = 64)
    private String keyWrapAlgorithm;

    @Column(length = 64)
    private String storageKeyVersion;

    private Long ciphertextLengthBytes;

    @Column(nullable = false, length = 512)
    private String hashDigest;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String digitalSignature;

    @Column(nullable = false, length = 64)
    private String signatureAlgorithm;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SubmissionVerificationStatus verificationStatus;

    @Column(length = 512)
    private String verificationMessage;
}

