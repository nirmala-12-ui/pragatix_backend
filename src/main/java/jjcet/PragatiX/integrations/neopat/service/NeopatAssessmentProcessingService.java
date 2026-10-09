package jjcet.PragatiX.integrations.neopat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jjcet.PragatiX.integrations.neopat.dto.NeopatAssessmentPayloadDto;
import jjcet.PragatiX.integrations.neopat.entity.CurrentAssessment;
import jjcet.PragatiX.integrations.neopat.entity.FailedAssessment;
import jjcet.PragatiX.integrations.neopat.repository.CurrentAssessmentRepository;
import jjcet.PragatiX.integrations.neopat.repository.FailedAssessmentRepository;
import jjcet.PragatiX.integrations.neopat.util.NeopatDateTimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NeopatAssessmentProcessingService {

    private static final Logger log = LoggerFactory.getLogger(NeopatAssessmentProcessingService.class);

    private final CurrentAssessmentRepository currentAssessmentRepository;
    private final FailedAssessmentRepository failedAssessmentRepository;
    private final NeopatAuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    public NeopatAssessmentProcessingService(CurrentAssessmentRepository currentAssessmentRepository,
                                             FailedAssessmentRepository failedAssessmentRepository,
                                             NeopatAuditLogService auditLogService,
                                             ObjectMapper objectMapper) {
        this.currentAssessmentRepository = currentAssessmentRepository;
        this.failedAssessmentRepository = failedAssessmentRepository;
        this.auditLogService = auditLogService;
        this.objectMapper = objectMapper;
    }

    @Transactional("neopatTransactionManager")
    public CurrentAssessment receiveAndStoreAssessment(NeopatAssessmentPayloadDto payload, String rawPayload) {
        CurrentAssessment entity = new CurrentAssessment();
        entity.setEmail(payload.getEmail().trim().toLowerCase());
        entity.setTestId(payload.getTestId().trim());
        entity.setMarks(payload.getMarks());
        entity.setTotalMarks(payload.getTotalMarks());
        entity.setAttempts(payload.getAttempts() != null ? payload.getAttempts() : 1);
        entity.setResultAnalysisUrl(payload.getResultAnalysisUrl());
        entity.setStartTime(payload.getStartTime());
        entity.setSubmitTime(payload.getSubmitTime());
        entity.setProcessingStatus("PENDING");
        entity.setReceivedAt(NeopatDateTimeUtil.nowIst());
        entity.setCreatedAt(NeopatDateTimeUtil.nowIst());

        if (payload.getSectionWiseMarks() != null) {
            try {
                entity.setSectionWiseMarks(objectMapper.writeValueAsString(payload.getSectionWiseMarks()));
            } catch (Exception e) {
                log.warn("Failed to serialize section_wise_marks: {}", e.getMessage());
            }
        }

        if (rawPayload != null && !rawPayload.trim().isEmpty()) {
            entity.setRawPayload(rawPayload);
        } else {
            try {
                entity.setRawPayload(objectMapper.writeValueAsString(payload));
            } catch (Exception ignored) {
            }
        }

        CurrentAssessment saved = currentAssessmentRepository.save(entity);

        auditLogService.logEvent(
                "CURRENT_ASSESSMENT",
                String.valueOf(saved.getId()),
                "NEOPAT_ASSESSMENT_RECEIVED",
                "SUCCESS",
                "Received Neopat assessment for email: " + saved.getEmail() + ", test: " + saved.getTestId(),
                null
        );

        return saved;
    }

    @Transactional("neopatTransactionManager")
    public CurrentAssessment receiveAndStoreAssessment(NeopatAssessmentPayloadDto payload) {
        return receiveAndStoreAssessment(payload, null);
    }

    @Transactional("neopatTransactionManager")
    public void recordRawPayloadFailure(String rawPayload, String failureType, String reason, String email) {
        try {
            FailedAssessment failure = new FailedAssessment();
            failure.setEmail(email != null && !email.isBlank() ? email : "unknown@neopat.in");
            failure.setFailureType(failureType);
            failure.setFailureReason(reason);
            failure.setRawPayload(rawPayload);
            failure.setRetryCount(0);
            failure.setStatus("FAILED");
            failure.setCreatedAt(NeopatDateTimeUtil.nowIst());

            failedAssessmentRepository.save(failure);

            auditLogService.logEvent(
                    "FAILED_ASSESSMENT",
                    String.valueOf(failure.getId()),
                    "NEOPAT_PAYLOAD_PARSE_FAILED",
                    "FAILED",
                    "Payload failure (" + failureType + "): " + reason,
                    null
            );
        } catch (Exception e) {
            log.error("Failed to record raw payload failure: {}", e.getMessage(), e);
        }
    }
}
