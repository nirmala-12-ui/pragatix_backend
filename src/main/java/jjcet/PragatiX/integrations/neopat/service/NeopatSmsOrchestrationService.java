package jjcet.PragatiX.integrations.neopat.service;

import jjcet.PragatiX.entity.Student;
import jjcet.PragatiX.integrations.neopat.config.TwilioConfigProperties;
import jjcet.PragatiX.integrations.neopat.dto.*;
import jjcet.PragatiX.integrations.neopat.entity.*;
import jjcet.PragatiX.integrations.neopat.repository.*;
import jjcet.PragatiX.integrations.neopat.util.NeopatDateTimeUtil;
import jjcet.PragatiX.modules.student.repository.StudentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class NeopatSmsOrchestrationService {

    private static final Logger log = LoggerFactory.getLogger(NeopatSmsOrchestrationService.class);
    private static final ZoneId KOLKATA_ZONE = ZoneId.of("Asia/Kolkata");
    private static final List<Integer> SUPPORTED_BATCHES = List.of(2027, 2028, 2029, 2030);

    private final BatchAssessmentRepositoryProvider repositoryProvider;
    private final AssessmentHistoryRepository assessmentHistoryRepository;
    private final FailedAssessmentRepository failedAssessmentRepository;
    private final ParentContactRepository parentContactRepository;
    private final NeopatSmsScheduleRepository scheduleRepository;
    private final NeopatSmsExecutionRepository executionRepository;
    private final NeopatAuditLogService auditLogService;
    private final NeopatTwilioSmsService twilioSmsService;
    private final StudentRepository studentRepository; // DB-1 PragatiX (Read-only)
    private final TwilioConfigProperties twilioConfig;

    public NeopatSmsOrchestrationService(
            BatchAssessmentRepositoryProvider repositoryProvider,
            AssessmentHistoryRepository assessmentHistoryRepository,
            FailedAssessmentRepository failedAssessmentRepository,
            ParentContactRepository parentContactRepository,
            NeopatSmsScheduleRepository scheduleRepository,
            NeopatSmsExecutionRepository executionRepository,
            NeopatAuditLogService auditLogService,
            NeopatTwilioSmsService twilioSmsService,
            StudentRepository studentRepository,
            TwilioConfigProperties twilioConfig) {
        this.repositoryProvider = repositoryProvider;
        this.assessmentHistoryRepository = assessmentHistoryRepository;
        this.failedAssessmentRepository = failedAssessmentRepository;
        this.parentContactRepository = parentContactRepository;
        this.scheduleRepository = scheduleRepository;
        this.executionRepository = executionRepository;
        this.auditLogService = auditLogService;
        this.twilioSmsService = twilioSmsService;
        this.studentRepository = studentRepository;
        this.twilioConfig = twilioConfig;
    }

    public static class BatchExecutionResult {
        public final int successCount;
        public final int failureCount;

        public BatchExecutionResult(int successCount, int failureCount) {
            this.successCount = successCount;
            this.failureCount = failureCount;
        }
    }

    /**
     * Executes the weekly SMS dispatch batch specifically reading from the batch's physical table (assessment_202X).
     */
    public BatchExecutionResult processPendingAssessmentsBatch(Integer batchYear) {
        log.info("Starting Neopat SMS batch dispatch reading from assessment_{}...", batchYear);
        List<? extends BaseBatchAssessment> pendingList = repositoryProvider.findPendingByBatch(batchYear);
        return processAssessmentList(pendingList, batchYear);
    }

    /**
     * Bulk process across all supported batch physical tables.
     */
    public BatchExecutionResult processPendingAssessmentsBatch() {
        int totalSuccess = 0;
        int totalFailure = 0;
        for (Integer batch : SUPPORTED_BATCHES) {
            BatchExecutionResult r = processPendingAssessmentsBatch(batch);
            totalSuccess += r.successCount;
            totalFailure += r.failureCount;
        }
        return new BatchExecutionResult(totalSuccess, totalFailure);
    }

    private BatchExecutionResult processAssessmentList(List<? extends BaseBatchAssessment> pendingList, Integer batchYear) {
        int successCount = 0;
        int failureCount = 0;

        if (pendingList == null || pendingList.isEmpty()) {
            return new BatchExecutionResult(0, 0);
        }

        for (BaseBatchAssessment assessment : pendingList) {
            Integer effectiveBatch = assessment.getBatchYear() != null ? assessment.getBatchYear() : batchYear;

            try {
                // 1. Lookup parent contact in DB-2 (neopa_sms) - PragatiX DB decoupled
                Optional<ParentContact> parentContactOpt =
                        parentContactRepository.findByStudentEmailAndIsActiveTrue(assessment.getEmail());
                if (parentContactOpt.isEmpty()) {
                    log.warn("Parent contact not found or inactive in neopa_sms for email: {}", assessment.getEmail());
                    recordFailedAssessment(assessment, "PARENT_CONTACT_NOT_FOUND",
                            "Parent contact not found or inactive for email: " + assessment.getEmail());
                    failureCount++;
                    continue;
                }
                ParentContact parentContact = parentContactOpt.get();

                // 2. Resolve student display name (fallback gracefully if not in PragatiX DB)
                String studentName = "Student";
                try {
                    Optional<Student> studentOpt = studentRepository.findByEmail(assessment.getEmail());
                    if (studentOpt.isPresent() && StringUtils.hasText(studentOpt.get().getFullName())) {
                        studentName = studentOpt.get().getFullName();
                    } else if (assessment.getEmail() != null && assessment.getEmail().contains("@")) {
                        studentName = assessment.getEmail().substring(0, assessment.getEmail().indexOf("@"));
                    }
                } catch (Exception ex) {
                    if (assessment.getEmail() != null && assessment.getEmail().contains("@")) {
                        studentName = assessment.getEmail().substring(0, assessment.getEmail().indexOf("@"));
                    }
                }

                // 3. Build SMS message content
                String testId = assessment.getTestName() != null ? assessment.getTestName() : assessment.getTestId();
                String scoreStr = assessment.getMarks() + "/" + assessment.getTotalMarks();
                String analysisUrl = StringUtils.hasText(assessment.getResultAnalysisUrl()) ? assessment.getResultAnalysisUrl() : "N/A";

                String smsMessage = String.format(
                        "Dear Parent, %s scored %s in Neopat test [%s]. Report: %s. - PragatiX",
                        studentName, scoreStr, testId, analysisUrl
                );

                // 4. Send SMS via Twilio (decrypting parent mobile safely if needed)
                String rawMobile = parentContact.getParentMobile();
                String plainMobile = (rawMobile != null && rawMobile.startsWith(jjcet.PragatiX.util.crypto.AesGcmEncryptionUtil.PREFIX))
                        ? jjcet.PragatiX.util.crypto.AesGcmEncryptionUtil.decrypt(rawMobile)
                        : rawMobile;

                NeopatTwilioSmsService.TwilioSendResult twilioResult =
                        twilioSmsService.sendSms(plainMobile, smsMessage);

                String maskedMobile = jjcet.PragatiX.integrations.neopat.dto.ParentContactResponseDto.maskMobile(plainMobile);

                // 5. CRITICAL RULE: Assessment Failure != SMS Failure
                if (twilioResult.isSuccess()) {
                    archiveToHistory(assessment, "SENT", null);
                    auditLogService.logEvent("ASSESSMENT_" + effectiveBatch, String.valueOf(assessment.getId()),
                            "SMS_SENT", "SUCCESS", "SMS successfully dispatched for Batch " + effectiveBatch + " to " + maskedMobile, null);
                    successCount++;
                } else {
                    log.error("Twilio SMS failed for assessment {} (Batch {}): {}", assessment.getId(), effectiveBatch, twilioResult.getErrorMessage());
                    archiveToHistory(assessment, "FAILED", twilioResult.getErrorMessage());
                    auditLogService.logEvent("ASSESSMENT_" + effectiveBatch, String.valueOf(assessment.getId()),
                            "SMS_FAILED", "FAILED", "Twilio dispatch error for Batch " + effectiveBatch + ": " + twilioResult.getErrorMessage(), null);
                    failureCount++;
                }

                // 6. Safely remove processed assessment from batch physical table
                repositoryProvider.delete(assessment);

            } catch (Exception ex) {
                log.error("Unexpected error processing assessment id {} (Batch {}): {}", assessment.getId(), effectiveBatch, ex.getMessage(), ex);
                recordFailedAssessment(assessment, "DATABASE_ERROR", "Unexpected processing error: " + ex.getMessage());
                repositoryProvider.delete(assessment);
                failureCount++;
            }
        }

        log.info("Completed Neopat SMS dispatch for Batch {}. Success: {}, Failures: {}", batchYear, successCount, failureCount);
        return new BatchExecutionResult(successCount, failureCount);
    }

    @Transactional("neopatTransactionManager")
    public void recordFailedAssessment(BaseBatchAssessment assessment, String failureType, String reason) {
        FailedAssessment failure = new FailedAssessment();
        failure.setSourceAssessmentId(assessment.getId());
        failure.setEmail(assessment.getEmail());
        failure.setTestId(assessment.getTestId());
        failure.setMarks(assessment.getMarks());
        failure.setTotalMarks(assessment.getTotalMarks());
        failure.setAttempts(assessment.getAttempts());
        failure.setResultAnalysisUrl(assessment.getResultAnalysisUrl());
        failure.setStartTime(assessment.getStartTime());
        failure.setSubmitTime(assessment.getSubmitTime());
        failure.setSectionWiseMarks(assessment.getSectionWiseMarks());
        failure.setFailureType(failureType);
        failure.setFailureReason(reason);
        failure.setRetryCount(0);
        failure.setStatus("FAILED");
        failure.setCreatedAt(NeopatDateTimeUtil.nowIst());

        failedAssessmentRepository.save(failure);
        repositoryProvider.delete(assessment);

        auditLogService.logEvent(
                "FAILED_ASSESSMENT",
                String.valueOf(failure.getId()),
                "NEOPAT_ASSESSMENT_FAILED",
                "FAILED",
                "Failure (" + failureType + "): " + reason,
                null
        );
    }

    @Transactional("neopatTransactionManager")
    public void archiveToHistory(BaseBatchAssessment assessment, String smsStatus, String failureReason) {
        AssessmentHistory history = new AssessmentHistory();
        history.setSourceAssessmentId(assessment.getId());
        history.setBatchYear(assessment.getBatchYear() != null ? assessment.getBatchYear() : 2027);
        history.setEmail(assessment.getEmail());
        history.setTestId(assessment.getTestId());
        history.setTestName(assessment.getTestName());
        history.setMarks(assessment.getMarks());
        history.setTotalMarks(assessment.getTotalMarks());
        history.setAttempts(assessment.getAttempts());
        history.setResultAnalysisUrl(assessment.getResultAnalysisUrl());
        history.setStartTime(assessment.getStartTime());
        history.setSubmitTime(assessment.getSubmitTime());
        history.setSectionWiseMarks(assessment.getSectionWiseMarks());
        history.setSmsStatus(smsStatus);
        history.setSmsFailureReason(failureReason);
        if ("SENT".equalsIgnoreCase(smsStatus)) {
            history.setSmsSentAt(NeopatDateTimeUtil.nowIst());
        }
        history.setProcessedAt(NeopatDateTimeUtil.nowIst());
        history.setCreatedAt(NeopatDateTimeUtil.nowIst());

        assessmentHistoryRepository.save(history);
        auditLogService.logEvent(
                "ASSESSMENT_HISTORY",
                String.valueOf(history.getId()),
                "ASSESSMENT_ARCHIVED",
                "SUCCESS",
                "Archived Batch " + history.getBatchYear() + " assessment with SMS status: " + smsStatus,
                null
        );
    }

    /**
     * Retrieve batch SMS settings and execution stats for all supported batches (2027-2030).
     */
    @Transactional("neopatTransactionManager")
    public List<NeopatBatchSmsConfigDto> getAllBatchSettings() {
        List<NeopatBatchSmsConfigDto> result = new ArrayList<>();
        for (Integer batch : SUPPORTED_BATCHES) {
            result.add(getBatchSettings(batch));
        }
        return result;
    }

    /**
     * Retrieve SMS schedule configuration and metrics for a specific batch year.
     */
    @Transactional("neopatTransactionManager")
    public NeopatBatchSmsConfigDto getBatchSettings(Integer batchYear) {
        NeopatSmsSchedule schedule = scheduleRepository.findByBatchYear(batchYear)
                .orElseGet(() -> {
                    NeopatSmsSchedule defaultSchedule = new NeopatSmsSchedule(batchYear, "SATURDAY", LocalTime.of(10, 0, 0), false);
                    return scheduleRepository.saveAndFlush(defaultSchedule);
                });

        long eligibleCount = repositoryProvider.countPendingByBatch(batchYear);
        long processedCount = assessmentHistoryRepository.countByBatchYear(batchYear);

        Optional<NeopatSmsExecution> lastExecOpt = executionRepository.findTopByBatchYearOrderByStartedAtDesc(batchYear);
        String lastExecutionStatus = lastExecOpt.map(NeopatSmsExecution::getStatus).orElse("NEVER_RUN");
        String lastExecutionTime = lastExecOpt.map(e -> e.getStartedAt() != null ? e.getStartedAt().toString() : null).orElse(null);

        String nextExecution = calculateNextExecution(schedule.getDayOfWeek(), schedule.getSendTime());

        return new NeopatBatchSmsConfigDto(
                schedule.getBatchYear(),
                schedule.getId(),
                schedule.getEnabled(),
                schedule.getDayOfWeek(),
                schedule.getSendTime().toString(),
                schedule.getTimezone(),
                schedule.getUpdatedAt() != null ? schedule.getUpdatedAt().toString() : null,
                schedule.getUpdatedBy(),
                nextExecution,
                lastExecutionStatus,
                lastExecutionTime,
                eligibleCount,
                processedCount
        );
    }

    /**
     * Update SMS schedule for a specific batch year.
     */
    @Transactional("neopatTransactionManager")
    public NeopatBatchSmsConfigDto updateBatchSettings(Integer batchYear, NeopatSmsScheduleDto dto, String updatedBy) {
        NeopatSmsSchedule schedule = scheduleRepository.findByBatchYear(batchYear)
                .orElseGet(() -> new NeopatSmsSchedule(batchYear, "SATURDAY", LocalTime.of(10, 0, 0), false));

        schedule.setBatchYear(batchYear);
        schedule.setEnabled(dto.getEnabled());
        schedule.setDayOfWeek(dto.getDayOfWeek().toUpperCase());
        schedule.setSendTime(LocalTime.parse(dto.getSendTime()));
        schedule.setTimezone("Asia/Kolkata");
        schedule.setUpdatedBy(updatedBy);
        schedule.setUpdatedAt(LocalDateTime.now(KOLKATA_ZONE));

        NeopatSmsSchedule saved = scheduleRepository.saveAndFlush(schedule);

        auditLogService.logEvent(
                "SMS_SCHEDULE",
                String.valueOf(saved.getId()),
                "SMS_SCHEDULE_UPDATED",
                "SUCCESS",
                "Admin updated schedule for Batch " + batchYear + ": " + saved.getDayOfWeek() + " " + saved.getSendTime() + ", enabled: " + saved.getEnabled(),
                null
        );

        return getBatchSettings(batchYear);
    }

    /**
     * Manually trigger an SMS dispatch run for a specific batch year.
     */
    @Transactional("neopatTransactionManager")
    public BatchExecutionResult triggerManualBatchRun(Integer batchYear, String triggeredBy) {
        log.info("Manual SMS run triggered for Batch {} by {}", batchYear, triggeredBy);
        ZonedDateTime nowKolkata = ZonedDateTime.now(KOLKATA_ZONE);
        LocalDate currentDate = nowKolkata.toLocalDate();
        LocalTime currentTime = nowKolkata.toLocalTime().withSecond(0).withNano(0);

        NeopatSmsExecution execution = new NeopatSmsExecution(batchYear, currentDate, currentTime, "IN_PROGRESS");
        execution = executionRepository.save(execution);

        auditLogService.logEvent(
                "SMS_EXECUTION",
                String.valueOf(execution.getId()),
                "MANUAL_BATCH_TRIGGERED",
                "IN_PROGRESS",
                "Manual SMS dispatch triggered for Batch " + batchYear + " by " + triggeredBy,
                null
        );

        try {
            BatchExecutionResult result = processPendingAssessmentsBatch(batchYear);
            execution.setStatus("COMPLETED");
            execution.setCompletedAt(LocalDateTime.now(KOLKATA_ZONE));
            execution.setSuccessCount(result.successCount);
            execution.setFailureCount(result.failureCount);
            executionRepository.save(execution);

            auditLogService.logEvent(
                    "SMS_EXECUTION",
                    String.valueOf(execution.getId()),
                    "MANUAL_BATCH_COMPLETED",
                    "SUCCESS",
                    String.format("Completed manual SMS run for Batch %d. Success: %d, Failures: %d", batchYear, result.successCount, result.failureCount),
                    null
            );

            return result;
        } catch (Exception e) {
            log.error("Manual SMS dispatch failed for Batch {}: {}", batchYear, e.getMessage(), e);
            execution.setStatus("FAILED");
            execution.setCompletedAt(LocalDateTime.now(KOLKATA_ZONE));
            executionRepository.save(execution);
            auditLogService.logEvent(
                    "SMS_EXECUTION",
                    String.valueOf(execution.getId()),
                    "MANUAL_BATCH_FAILED",
                    "FAILED",
                    "Manual run failed for Batch " + batchYear + ": " + e.getMessage(),
                    null
            );
            throw e;
        }
    }

    private String calculateNextExecution(String dayOfWeekStr, LocalTime sendTime) {
        if (dayOfWeekStr == null || sendTime == null) return null;
        try {
            DayOfWeek targetDay = DayOfWeek.valueOf(dayOfWeekStr.toUpperCase());
            ZonedDateTime now = ZonedDateTime.now(KOLKATA_ZONE);
            ZonedDateTime nextDate = now.with(TemporalAdjusters.nextOrSame(targetDay))
                    .withHour(sendTime.getHour())
                    .withMinute(sendTime.getMinute())
                    .withSecond(0)
                    .withNano(0);

            if (nextDate.isBefore(now) || nextDate.isEqual(now)) {
                nextDate = nextDate.plusWeeks(1);
            }
            return nextDate.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        } catch (Exception e) {
            return null;
        }
    }

    @Transactional("neopatTransactionManager")
    public NeopatSmsSettingsResponseDto getSettings() {
        NeopatSmsSchedule schedule = scheduleRepository.findFirstByOrderByIdAsc()
                .orElseGet(() -> {
                    NeopatSmsSchedule defaultSchedule = new NeopatSmsSchedule(2027, "SATURDAY", LocalTime.of(10, 0, 0), false);
                    return scheduleRepository.save(defaultSchedule);
                });

        return new NeopatSmsSettingsResponseDto(
                schedule.getId(),
                schedule.getEnabled(),
                schedule.getDayOfWeek(),
                schedule.getSendTime().toString(),
                schedule.getTimezone(),
                schedule.getUpdatedAt() != null ? schedule.getUpdatedAt().toString() : null,
                schedule.getUpdatedBy()
        );
    }

    @Transactional("neopatTransactionManager")
    public NeopatSmsSettingsResponseDto updateSettings(NeopatSmsScheduleDto dto, String updatedBy) {
        NeopatSmsSchedule schedule = scheduleRepository.findFirstByOrderByIdAsc()
                .orElseGet(() -> new NeopatSmsSchedule(2027, "SATURDAY", LocalTime.of(10, 0, 0), false));

        schedule.setEnabled(dto.getEnabled());
        schedule.setDayOfWeek(dto.getDayOfWeek().toUpperCase());
        schedule.setSendTime(LocalTime.parse(dto.getSendTime()));
        schedule.setTimezone("Asia/Kolkata");
        schedule.setUpdatedBy(updatedBy);
        schedule.setUpdatedAt(LocalDateTime.now(KOLKATA_ZONE));

        NeopatSmsSchedule saved = scheduleRepository.save(schedule);

        auditLogService.logEvent(
                "SMS_SCHEDULE",
                String.valueOf(saved.getId()),
                "SMS_SCHEDULE_UPDATED",
                "SUCCESS",
                "Super Admin updated schedule: " + saved.getDayOfWeek() + " " + saved.getSendTime() + ", enabled: " + saved.getEnabled(),
                null
        );

        return new NeopatSmsSettingsResponseDto(
                saved.getId(),
                saved.getEnabled(),
                saved.getDayOfWeek(),
                saved.getSendTime().toString(),
                saved.getTimezone(),
                saved.getUpdatedAt().toString(),
                saved.getUpdatedBy()
        );
    }

    public NeopatTestSmsResponseDto sendManualTestSms(NeopatTestSmsRequestDto request, String triggeredBy) {
        String targetPhone = StringUtils.hasText(request.getPhoneNumber())
                ? request.getPhoneNumber()
                : twilioConfig.getTestPhoneNumber();

        if (!StringUtils.hasText(targetPhone)) {
            return new NeopatTestSmsResponseDto(false, null, null, "Target test phone number is not configured.");
        }

        String message = StringUtils.hasText(request.getTestMessage())
                ? request.getTestMessage()
                : "PragatiX - Neopat Parent SMS Test Message. Dispatched at: " + LocalDateTime.now(KOLKATA_ZONE);

        NeopatTwilioSmsService.TwilioSendResult result = twilioSmsService.sendSms(targetPhone, message);

        auditLogService.logEvent(
                "MANUAL_TEST",
                targetPhone,
                "MANUAL_SMS_TEST",
                result.isSuccess() ? "SUCCESS" : "FAILED",
                "Triggered by: " + triggeredBy + ", recipient: " + targetPhone + ", SID: " + result.getMessageSid(),
                null
        );

        return new NeopatTestSmsResponseDto(
                result.isSuccess(),
                targetPhone,
                result.getMessageSid(),
                result.isSuccess() ? "Test SMS queued/sent successfully" : result.getErrorMessage()
        );
    }
}
