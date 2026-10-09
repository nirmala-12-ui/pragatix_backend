package jjcet.PragatiX.integrations.neopat.service;

import jjcet.PragatiX.entity.Student;
import jjcet.PragatiX.integrations.neopat.config.TwilioConfigProperties;
import jjcet.PragatiX.integrations.neopat.dto.*;
import jjcet.PragatiX.integrations.neopat.entity.*;
import jjcet.PragatiX.integrations.neopat.repository.*;
import jjcet.PragatiX.modules.student.repository.StudentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
public class NeopatSmsOrchestrationService {

    private static final Logger log = LoggerFactory.getLogger(NeopatSmsOrchestrationService.class);

    private final CurrentAssessmentRepository currentAssessmentRepository;
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
            CurrentAssessmentRepository currentAssessmentRepository,
            AssessmentHistoryRepository assessmentHistoryRepository,
            FailedAssessmentRepository failedAssessmentRepository,
            ParentContactRepository parentContactRepository,
            NeopatSmsScheduleRepository scheduleRepository,
            NeopatSmsExecutionRepository executionRepository,
            NeopatAuditLogService auditLogService,
            NeopatTwilioSmsService twilioSmsService,
            StudentRepository studentRepository,
            TwilioConfigProperties twilioConfig) {
        this.currentAssessmentRepository = currentAssessmentRepository;
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
     * Executes the weekly SMS dispatch batch with full cross-database coordination.
     */
    public BatchExecutionResult processPendingAssessmentsBatch() {
        log.info("Starting Neopat SMS batch dispatch...");
        List<CurrentAssessment> pendingList = currentAssessmentRepository.findByProcessingStatus("PENDING");

        int successCount = 0;
        int failureCount = 0;

        for (CurrentAssessment assessment : pendingList) {
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
                String testId = assessment.getTestId();
                String scoreStr = assessment.getMarks() + "/" + assessment.getTotalMarks();
                String analysisUrl = StringUtils.hasText(assessment.getResultAnalysisUrl()) ? assessment.getResultAnalysisUrl() : "N/A";

                String smsMessage = String.format(
                        "Dear Parent, %s scored %s in Neopat test [%s]. Report: %s. - PragatiX",
                        studentName, scoreStr, testId, analysisUrl
                );

                // 4. Send SMS via Twilio
                NeopatTwilioSmsService.TwilioSendResult twilioResult =
                        twilioSmsService.sendSms(parentContact.getParentMobile(), smsMessage);

                // 5. CRITICAL RULE: Assessment Failure != SMS Failure
                if (twilioResult.isSuccess()) {
                    archiveToHistory(assessment, "SENT", null);
                    auditLogService.logEvent("CURRENT_ASSESSMENT", String.valueOf(assessment.getId()),
                            "SMS_SENT", "SUCCESS", "SMS successfully dispatched to " + parentContact.getParentMobile(), null);
                    successCount++;
                } else {
                    log.error("Twilio SMS failed for assessment {}: {}", assessment.getId(), twilioResult.getErrorMessage());
                    // Still archived to history with sms_status = 'FAILED' (NOT failed_assessment!)
                    archiveToHistory(assessment, "FAILED", twilioResult.getErrorMessage());
                    auditLogService.logEvent("CURRENT_ASSESSMENT", String.valueOf(assessment.getId()),
                            "SMS_FAILED", "FAILED", "Twilio dispatch error: " + twilioResult.getErrorMessage(), null);
                    failureCount++;
                }

                // 6. Safely remove processed assessment from current_assessment
                currentAssessmentRepository.delete(assessment);

            } catch (Exception ex) {
                log.error("Unexpected error processing assessment id {}: {}", assessment.getId(), ex.getMessage(), ex);
                recordFailedAssessment(assessment, "DATABASE_ERROR", "Unexpected processing error: " + ex.getMessage());
                currentAssessmentRepository.delete(assessment);
                failureCount++;
            }
        }

        log.info("Completed Neopat SMS batch. Success: {}, Failures: {}", successCount, failureCount);
        return new BatchExecutionResult(successCount, failureCount);
    }

    @Transactional("neopatTransactionManager")
    public void recordFailedAssessment(CurrentAssessment assessment, String failureType, String reason) {
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
        failure.setRawPayload(assessment.getRawPayload());
        failure.setFailureType(failureType);
        failure.setFailureReason(reason);
        failure.setRetryCount(0);
        failure.setStatus("FAILED");
        failure.setCreatedAt(jjcet.PragatiX.integrations.neopat.util.NeopatDateTimeUtil.nowIst());

        failedAssessmentRepository.save(failure);
        currentAssessmentRepository.delete(assessment);

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
    public void archiveToHistory(CurrentAssessment assessment, String smsStatus, String failureReason) {
        AssessmentHistory history = new AssessmentHistory();
        history.setSourceAssessmentId(assessment.getId());
        history.setEmail(assessment.getEmail());
        history.setTestId(assessment.getTestId());
        history.setMarks(assessment.getMarks());
        history.setTotalMarks(assessment.getTotalMarks());
        history.setAttempts(assessment.getAttempts());
        history.setResultAnalysisUrl(assessment.getResultAnalysisUrl());
        history.setStartTime(assessment.getStartTime());
        history.setSubmitTime(assessment.getSubmitTime());
        history.setSectionWiseMarks(assessment.getSectionWiseMarks());
        history.setRawPayload(assessment.getRawPayload());
        history.setSmsStatus(smsStatus);
        history.setSmsFailureReason(failureReason);
        if ("SENT".equalsIgnoreCase(smsStatus)) {
            history.setSmsSentAt(jjcet.PragatiX.integrations.neopat.util.NeopatDateTimeUtil.nowIst());
        }
        history.setProcessedAt(jjcet.PragatiX.integrations.neopat.util.NeopatDateTimeUtil.nowIst());
        history.setCreatedAt(jjcet.PragatiX.integrations.neopat.util.NeopatDateTimeUtil.nowIst());

        assessmentHistoryRepository.save(history);
        auditLogService.logEvent(
                "ASSESSMENT_HISTORY",
                String.valueOf(history.getId()),
                "ASSESSMENT_ARCHIVED",
                "SUCCESS",
                "Archived assessment history with SMS status: " + smsStatus,
                null
        );
    }

    @Transactional("neopatTransactionManager")
    public NeopatSmsSettingsResponseDto getSettings() {
        NeopatSmsSchedule schedule = scheduleRepository.findFirstByOrderByIdAsc()
                .orElseGet(() -> {
                    NeopatSmsSchedule defaultSchedule = new NeopatSmsSchedule();
                    defaultSchedule.setDayOfWeek("SATURDAY");
                    defaultSchedule.setSendTime(LocalTime.of(10, 0, 0));
                    defaultSchedule.setEnabled(false);
                    defaultSchedule.setTimezone("Asia/Kolkata");
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
                .orElseGet(NeopatSmsSchedule::new);

        schedule.setEnabled(dto.getEnabled());
        schedule.setDayOfWeek(dto.getDayOfWeek().toUpperCase());
        schedule.setSendTime(LocalTime.parse(dto.getSendTime()));
        schedule.setTimezone("Asia/Kolkata");
        schedule.setUpdatedBy(updatedBy);
        schedule.setUpdatedAt(LocalDateTime.now());

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
                : "PragatiX - Neopat Parent SMS Test Message. Dispatched at: " + LocalDateTime.now();

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
