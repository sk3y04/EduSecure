package edusecure.edusecure.repository.audit;

import edusecure.edusecure.entity.audit.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    Optional<AuditLog> findTopByOrderByEventTimestampDesc();

    List<AuditLog> findByEntityTypeAndEntityIdOrderByEventTimestampAsc(String entityType, UUID entityId);
}