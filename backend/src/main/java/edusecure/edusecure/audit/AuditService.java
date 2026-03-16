package edusecure.edusecure.audit;

import edusecure.edusecure.entity.AuditActionType;
import edusecure.edusecure.entity.AuditLog;
import edusecure.edusecure.repository.AuditLogRepository;
import edusecure.edusecure.service.crypto.ICryptoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final ICryptoService cryptoService;

    @Transactional
    public AuditLog record(AuditActionType actionType, UUID actorUserId, String entityType, UUID entityId, String detailsJson) {
        String previousIntegrityValue = auditLogRepository.findTopByOrderByEventTimestampDesc()
                .map(AuditLog::getIntegrityValue)
                .orElse(null);

        Instant timestamp = Instant.now();
        String canonicalPayload = actionType + "|" + actorUserId + "|" + entityType + "|" + entityId + "|" + timestamp + "|" + detailsJson;
        String integrityValue = cryptoService.computeAuditMac(canonicalPayload, previousIntegrityValue);

        AuditLog auditLog = AuditLog.builder()
                .actionType(actionType)
                .actorUserId(actorUserId)
                .entityType(entityType)
                .entityId(entityId)
                .eventTimestamp(timestamp)
                .detailsJson(detailsJson)
                .integrityValue(integrityValue)
                .previousIntegrityValue(previousIntegrityValue)
                .build();

        return auditLogRepository.save(auditLog);
    }
}

