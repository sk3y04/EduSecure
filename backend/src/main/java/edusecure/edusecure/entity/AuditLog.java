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
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private AuditActionType actionType;

    @Column(nullable = false)
    private UUID actorUserId;

    @Column(nullable = false, length = 128)
    private String entityType;

    @Column(nullable = false)
    private UUID entityId;

    @Column(nullable = false)
    private Instant eventTimestamp;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String detailsJson;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String integrityValue;

    @Column(columnDefinition = "TEXT")
    private String previousIntegrityValue;
}

