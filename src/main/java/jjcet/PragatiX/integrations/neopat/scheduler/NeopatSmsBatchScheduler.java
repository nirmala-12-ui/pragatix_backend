package jjcet.PragatiX.integrations.neopat.scheduler;

import jjcet.PragatiX.integrations.neopat.entity.NeopatSmsExecution;
import jjcet.PragatiX.integrations.neopat.entity.NeopatSmsSchedule;
import jjcet.PragatiX.integrations.neopat.repository.NeopatSmsExecutionRepository;
import jjcet.PragatiX.integrations.neopat.repository.NeopatSmsScheduleRepository;
import jjcet.PragatiX.integrations.neopat.service.NeopatAuditLogService;
import jjcet.PragatiX.integrations.neopat.service.NeopatSmsOrchestrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Component
public class NeopatSmsBatchScheduler {

    private static final Logger log = LoggerFactory.getLogger(NeopatSmsBatchScheduler.class);
    private static final ZoneId KOLKATA_ZONE = ZoneId.of("Asia/Kolkata");

    private final NeopatSmsScheduleRepository scheduleRepository;
    private final NeopatSmsExecutionRepository executionRepository;
    private final NeopatSmsOrchestrationService orchestrationService;
    private final NeopatAuditLogService auditLogService;

    public NeopatSmsBatchScheduler(
            NeopatSmsScheduleRepository scheduleRepository,
            NeopatSmsExecutionRepository executionRepository,
            NeopatSmsOrchestrationService orchestrationService,
            NeopatAuditLogService auditLogService) {
        this.scheduleRepository = scheduleRepository;
        this.executionRepository = executionRepository;
        this.orchestrationService = orchestrationService;
        this.auditLogService = auditLogService;
    }

    /**
     * Periodic lightweight check every 60 seconds against neopa_sms.neopat_sms_schedule.
     * Evaluates schedule in Asia/Kolkata timezone and guarantees idempotency.
     */
    @Scheduled(cron = "0 * * * * *")
    public void evaluateAndExecuteSchedule() {
        Optional<NeopatSmsSchedule> scheduleOpt = scheduleRepository.findFirstByOrderByIdAsc();
        if (scheduleOpt.isEmpty()) {
            return;
        }

        NeopatSmsSchedule schedule = scheduleOpt.get();
        if (!Boolean.TRUE.equals(schedule.getEnabled())) {
            return;
        }

        ZonedDateTime nowKolkata = ZonedDateTime.now(KOLKATA_ZONE);
        DayOfWeek currentDay = nowKolkata.getDayOfWeek();
        LocalTime currentTime = nowKolkata.toLocalTime().truncatedTo(ChronoUnit.MINUTES);
        LocalDate currentDate = nowKolkata.toLocalDate();

        // 1. Verify Day of Week
        if (!currentDay.name().equalsIgnoreCase(schedule.getDayOfWeek())) {
            return;
        }

        // 2. Verify Time Window (matches minute)
        LocalTime targetTime = schedule.getSendTime().truncatedTo(ChronoUnit.MINUTES);
        if (currentTime.getHour() != targetTime.getHour() || currentTime.getMinute() != targetTime.getMinute()) {
            return;
        }

        // 3. Guarantee Idempotency using DB-2 neopat_sms_execution
        boolean alreadyExecuted = executionRepository.existsByScheduledDateAndScheduledTime(currentDate, targetTime);
        if (alreadyExecuted) {
            log.debug("Neopat SMS batch already executed for date: {} time: {}", currentDate, targetTime);
            return;
        }

        log.info("Triggering scheduled Neopat SMS batch for date: {} at time: {}", currentDate, targetTime);

        // Record execution start
        NeopatSmsExecution execution = new NeopatSmsExecution(currentDate, targetTime, "IN_PROGRESS");
        execution = executionRepository.save(execution);

        auditLogService.logEvent(
                "SMS_EXECUTION",
                String.valueOf(execution.getId()),
                "SMS_SCHEDULER_STARTED",
                "IN_PROGRESS",
                "Started weekly scheduled SMS dispatch batch for " + currentDate + " " + targetTime,
                null
        );

        try {
            NeopatSmsOrchestrationService.BatchExecutionResult result = orchestrationService.processPendingAssessmentsBatch();

            execution.setStatus("COMPLETED");
            execution.setCompletedAt(LocalDateTime.now());
            execution.setSuccessCount(result.successCount);
            execution.setFailureCount(result.failureCount);
            executionRepository.save(execution);

            auditLogService.logEvent(
                    "SMS_EXECUTION",
                    String.valueOf(execution.getId()),
                    "SMS_SCHEDULER_COMPLETED",
                    "SUCCESS",
                    String.format("Completed weekly SMS batch. Success: %d, Failures: %d", result.successCount, result.failureCount),
                    null
            );
        } catch (Exception e) {
            log.error("Fatal error during scheduled SMS batch: {}", e.getMessage(), e);
            execution.setStatus("FAILED");
            execution.setCompletedAt(LocalDateTime.now());
            executionRepository.save(execution);

            auditLogService.logEvent(
                    "SMS_EXECUTION",
                    String.valueOf(execution.getId()),
                    "SMS_SCHEDULER_FAILED",
                    "FAILED",
                    "Batch failed with error: " + e.getMessage(),
                    null
            );
        }
    }
}
