package jjcet.PragatiX.integrations.neopat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jjcet.PragatiX.integrations.neopat.dto.NeopatAssessmentPayloadDto;
import jjcet.PragatiX.integrations.neopat.entity.BaseBatchAssessment;
import jjcet.PragatiX.integrations.neopat.entity.FailedAssessment;
import jjcet.PragatiX.integrations.neopat.repository.FailedAssessmentRepository;
import jjcet.PragatiX.integrations.neopat.util.NeopatBatchAssessmentFilter;
import jjcet.PragatiX.integrations.neopat.util.NeopatDateTimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class NeopatAssessmentProcessingService {

    private static final Logger log = LoggerFactory.getLogger(NeopatAssessmentProcessingService.class);

    private final BatchAssessmentRepositoryProvider repositoryProvider;
    private final FailedAssessmentRepository failedAssessmentRepository;
    private final NeopatAuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    public record InboundAssessmentResult(boolean accepted, boolean skipped, String reason, Integer batchYear, BaseBatchAssessment assessment) {
        public static InboundAssessmentResult accept(Integer batchYear, BaseBatchAssessment assessment) {
            return new InboundAssessmentResult(true, false, "ACCEPTED", batchYear, assessment);
        }

        public static InboundAssessmentResult skip(String reason) {
            return new InboundAssessmentResult(false, true, reason, null, null);
        }
    }

    public NeopatAssessmentProcessingService(BatchAssessmentRepositoryProvider repositoryProvider,
                                             FailedAssessmentRepository failedAssessmentRepository,
                                             NeopatAuditLogService auditLogService,
                                             ObjectMapper objectMapper) {
        this.repositoryProvider = repositoryProvider;
        this.failedAssessmentRepository = failedAssessmentRepository;
        this.auditLogService = auditLogService;
        this.objectMapper = objectMapper;
    }

    /**
     * Primary processing entry point for incoming Neopat assessment webhook.
     * Evaluates test name, extracts batch year, filters out non-weekly/daily/unsupported assessments,
     * and stores valid assessments into the batch-specific table (assessment_2027..2030)
     * while avoiding duplicate entries.
     */
    @Transactional("neopatTransactionManager")
    public InboundAssessmentResult processInboundAssessment(NeopatAssessmentPayloadDto payload, String rawPayload) {
        String testIdentifier = payload.getEffectiveTestName();
        String email = payload.getEmail() != null ? payload.getEmail().trim().toLowerCase() : "";

        // 1. Evaluate batch year and weekly criteria
        NeopatBatchAssessmentFilter.FilterResult filterResult = NeopatBatchAssessmentFilter.evaluate(testIdentifier);
        if (!filterResult.accepted()) {
            log.info("Skipping assessment submission for student '{}', test '{}': {}", email, testIdentifier, filterResult.reason());
            auditLogService.logEvent(
                    "INBOUND_ASSESSMENT",
                    testIdentifier,
                    "ASSESSMENT_SKIPPED",
                    "SKIPPED",
                    "Skipped Neopat assessment [" + testIdentifier + "] for " + email + ": " + filterResult.reason(),
                    null
            );
            return InboundAssessmentResult.skip(filterResult.reason());
        }

        Integer batchYear = filterResult.batchYear();
        String testId = payload.getTestId() != null ? payload.getTestId().trim() : testIdentifier;

        // 2. Prevent duplicate entries using batch-specific table lookup
        Optional<? extends BaseBatchAssessment> existingOpt =
                repositoryProvider.findByBatchAndEmailAndTestId(batchYear, email, testId);

        BaseBatchAssessment entity = existingOpt.isPresent()
                ? existingOpt.get()
                : repositoryProvider.createNewEntity(batchYear);

        entity.setEmail(email);
        entity.setTestId(testId);
        entity.setTestName(testIdentifier);
        entity.setMarks(payload.getMarks());
        entity.setTotalMarks(payload.getTotalMarks());
        entity.setAttempts(payload.getAttempts() != null ? payload.getAttempts() : 1);
        entity.setResultAnalysisUrl(payload.getResultAnalysisUrl());
        entity.setStartTime(payload.getStartTime());
        entity.setSubmitTime(payload.getSubmitTime());
        entity.setProcessingStatus("PENDING");
        entity.setReceivedAt(NeopatDateTimeUtil.nowIst());

        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(NeopatDateTimeUtil.nowIst());
        }

        if (payload.getSectionWiseMarks() != null) {
            try {
                entity.setSectionWiseMarks(objectMapper.writeValueAsString(payload.getSectionWiseMarks()));
            } catch (Exception e) {
                log.warn("Failed to serialize section_wise_marks: {}", e.getMessage());
            }
        }

        BaseBatchAssessment saved = repositoryProvider.save(entity);

        auditLogService.logEvent(
                "ASSESSMENT_" + batchYear,
                String.valueOf(saved.getId()),
                "NEOPAT_ASSESSMENT_RECEIVED",
                "SUCCESS",
                "Stored in assessment_" + batchYear + " for email: " + saved.getEmail() + ", test: " + testIdentifier,
                null
        );

        return InboundAssessmentResult.accept(batchYear, saved);
    }

    @Transactional("neopatTransactionManager")
    public BaseBatchAssessment receiveAndStoreAssessment(NeopatAssessmentPayloadDto payload, String rawPayload) {
        InboundAssessmentResult result = processInboundAssessment(payload, rawPayload);
        return result.assessment();
    }

    @Transactional("neopatTransactionManager")
    public BaseBatchAssessment receiveAndStoreAssessment(NeopatAssessmentPayloadDto payload) {
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
