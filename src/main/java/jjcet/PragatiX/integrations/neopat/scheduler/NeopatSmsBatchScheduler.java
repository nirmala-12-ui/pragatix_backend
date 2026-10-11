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
import java.util.List;

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
     * Evaluates independent schedules for all batches (2027, 2028, 2029, 2030) in Asia/Kolkata timezone
     * and guarantees per-batch idempotency and fault isolation.
     */
    @Scheduled(cron = "0 * * * * *")
    public void evaluateAndExecuteSchedule() {
        List<NeopatSmsSchedule> schedules = scheduleRepository.findAllByOrderByBatchYearAsc();
        if (schedules.isEmpty()) {
            return;
        }

        ZonedDateTime nowKolkata = ZonedDateTime.now(KOLKATA_ZONE);
        DayOfWeek currentDay = nowKolkata.getDayOfWeek();
        LocalTime currentTime = nowKolkata.toLocalTime().truncatedTo(ChronoUnit.MINUTES);
        LocalDate currentDate = nowKolkata.toLocalDate();

        for (NeopatSmsSchedule schedule : schedules) {
            Integer batchYear = schedule.getBatchYear();
            if (batchYear == null) {
                continue;
            }

            // 1. Check enabled
            if (!Boolean.TRUE.equals(schedule.getEnabled())) {
                continue;
            }

            // 2. Verify Day of Week
            if (!currentDay.name().equalsIgnoreCase(schedule.getDayOfWeek())) {
                continue;
            }

            // 3. Verify Time Window (matches minute)
            LocalTime targetTime = schedule.getSendTime().truncatedTo(ChronoUnit.MINUTES);
            if (currentTime.getHour() != targetTime.getHour() || currentTime.getMinute() != targetTime.getMinute()) {
                continue;
            }

            // 4. Guarantee Idempotency per batch using neopat_sms_execution
            boolean alreadyExecuted = executionRepository.existsByBatchYearAndScheduledDateAndScheduledTime(batchYear, currentDate, targetTime);
            if (alreadyExecuted) {
                log.debug("Neopat SMS batch already executed for Batch: {} date: {} time: {}", batchYear, currentDate, targetTime);
                continue;
            }

            log.info("Triggering scheduled Neopat SMS batch for Batch: {} date: {} at time: {}", batchYear, currentDate, targetTime);

            // 5. Record execution start
            NeopatSmsExecution execution = new NeopatSmsExecution(batchYear, currentDate, targetTime, "IN_PROGRESS");
            execution = executionRepository.save(execution);

            auditLogService.logEvent(
                    "SMS_EXECUTION",
                    String.valueOf(execution.getId()),
                    "SMS_SCHEDULER_STARTED",
                    "IN_PROGRESS",
                    "Started weekly scheduled SMS dispatch for Batch " + batchYear + " on " + currentDate + " " + targetTime,
                    null
            );

            try {
                NeopatSmsOrchestrationService.BatchExecutionResult result = orchestrationService.processPendingAssessmentsBatch(batchYear);

                execution.setStatus("COMPLETED");
                execution.setCompletedAt(LocalDateTime.now(KOLKATA_ZONE));
                execution.setSuccessCount(result.successCount);
                execution.setFailureCount(result.failureCount);
                executionRepository.save(execution);

                auditLogService.logEvent(
                        "SMS_EXECUTION",
                        String.valueOf(execution.getId()),
                        "SMS_SCHEDULER_COMPLETED",
                        "SUCCESS",
                        String.format("Completed weekly SMS batch for Batch %d. Success: %d, Failures: %d", batchYear, result.successCount, result.failureCount),
                        null
                );
            } catch (Exception e) {
                log.error("Fatal error during scheduled SMS batch for Batch {}: {}", batchYear, e.getMessage(), e);
                execution.setStatus("FAILED");
                execution.setCompletedAt(LocalDateTime.now(KOLKATA_ZONE));
                executionRepository.save(execution);

                auditLogService.logEvent(
                        "SMS_EXECUTION",
                        String.valueOf(execution.getId()),
                        "SMS_SCHEDULER_FAILED",
                        "FAILED",
                        "Batch " + batchYear + " failed with error: " + e.getMessage(),
                        null
                );
            }
        }
    }
}
