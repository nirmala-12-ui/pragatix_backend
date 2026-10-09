package jjcet.PragatiX.integrations.neopat.service;

import jjcet.PragatiX.integrations.neopat.entity.NeopatAuditLog;
import jjcet.PragatiX.integrations.neopat.repository.NeopatAuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NeopatAuditLogService {

    private static final Logger log = LoggerFactory.getLogger(NeopatAuditLogService.class);

    private final NeopatAuditLogRepository auditLogRepository;

    public NeopatAuditLogService(NeopatAuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional("neopatTransactionManager")
    public void logEvent(String entityType, String entityId, String eventType, String status, String message, String metadataJson) {
        try {
            NeopatAuditLog audit = new NeopatAuditLog(entityType, entityId, eventType, status, message, metadataJson);
            auditLogRepository.save(audit);
            log.info("[NEOPAT AUDIT] {} - {} - status: {}", eventType, message, status);
        } catch (Exception e) {
            log.error("Failed to write to Neopat audit log: {}", e.getMessage(), e);
        }
    }
}
