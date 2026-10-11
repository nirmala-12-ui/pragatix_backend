package jjcet.PragatiX.integrations.neopat;

import com.fasterxml.jackson.databind.ObjectMapper;
import jjcet.PragatiX.entity.Student;
import jjcet.PragatiX.integrations.neopat.config.TwilioConfigProperties;
import jjcet.PragatiX.integrations.neopat.controller.NeopatInboundAssessmentController;
import jjcet.PragatiX.integrations.neopat.controller.NeopatSmsSettingsController;
import jjcet.PragatiX.integrations.neopat.dto.*;
import jjcet.PragatiX.integrations.neopat.entity.*;
import jjcet.PragatiX.integrations.neopat.repository.*;
import jjcet.PragatiX.integrations.neopat.scheduler.NeopatSmsBatchScheduler;
import jjcet.PragatiX.integrations.neopat.service.*;
import jjcet.PragatiX.integrations.neopat.util.NeopatBatchAssessmentFilter;
import jjcet.PragatiX.modules.student.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NeopatBatchIntegrationTest {

    @Mock
    private Assessment2027Repository repo2027;

    @Mock
    private Assessment2028Repository repo2028;

    @Mock
    private Assessment2029Repository repo2029;

    @Mock
    private Assessment2030Repository repo2030;

    @Mock
    private AssessmentHistoryRepository assessmentHistoryRepository;

    @Mock
    private FailedAssessmentRepository failedAssessmentRepository;

    @Mock
    private ParentContactRepository parentContactRepository;

    @Mock
    private NeopatSmsScheduleRepository scheduleRepository;

    @Mock
    private NeopatSmsExecutionRepository executionRepository;

    @Mock
    private NeopatAuditLogService auditLogService;

    @Mock
    private NeopatTwilioSmsService twilioSmsService;

    @Mock
    private StudentRepository studentRepository;

    private BatchAssessmentRepositoryProvider repositoryProvider;
    private ObjectMapper objectMapper;
    private TwilioConfigProperties twilioConfig;
    private NeopatAssessmentProcessingService processingService;
    private NeopatInboundAssessmentController inboundController;
    private NeopatSmsOrchestrationService orchestrationService;
    private NeopatSmsSettingsController settingsController;
    private NeopatSmsBatchScheduler scheduler;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        twilioConfig = new TwilioConfigProperties();
        twilioConfig.setTestPhoneNumber("+919876543210");
        twilioConfig.setFromNumber("+1234567890");

        repositoryProvider = new BatchAssessmentRepositoryProvider(repo2027, repo2028, repo2029, repo2030);

        processingService = new NeopatAssessmentProcessingService(
                repositoryProvider,
                failedAssessmentRepository,
                auditLogService,
                objectMapper
        );

        inboundController = new NeopatInboundAssessmentController(processingService, objectMapper);

        orchestrationService = new NeopatSmsOrchestrationService(
                repositoryProvider,
                assessmentHistoryRepository,
                failedAssessmentRepository,
                parentContactRepository,
                scheduleRepository,
                executionRepository,
                auditLogService,
                twilioSmsService,
                studentRepository,
                twilioConfig
        );

        settingsController = new NeopatSmsSettingsController(orchestrationService);
        scheduler = new NeopatSmsBatchScheduler(scheduleRepository, executionRepository, orchestrationService, auditLogService);
    }

    // 1. A valid 2029 weekly assessment being stored under physical table assessment_2029
    @Test
    @DisplayName("Scenario 1: Valid 2029 weekly assessment stored under physical table assessment_2029")
    void testValid2029WeeklyAssessmentStoredUnderBatch2029() {
        String payloadJson = """
                {
                  "email": "student2029@jjcet.ac.in",
                  "test_name": "2029_JJCET_08th_OCT_WEEKLY_ASSESSMENT",
                  "test_id": "TEST_001",
                  "marks": 85.5,
                  "total_marks": 100.0
                }
                """;

        when(repo2029.findByEmailAndTestId("student2029@jjcet.ac.in", "TEST_001"))
                .thenReturn(Optional.empty());
        when(repo2029.save(any(Assessment2029.class))).thenAnswer(i -> {
            Assessment2029 entity = i.getArgument(0);
            entity.setId(2901L);
            return entity;
        });

        ResponseEntity<Map<String, Boolean>> response = inboundController.receiveAssessment(payloadJson);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(Boolean.TRUE, response.getBody().get("success"));

        ArgumentCaptor<Assessment2029> captor = ArgumentCaptor.forClass(Assessment2029.class);
        verify(repo2029).save(captor.capture());
        Assessment2029 saved = captor.getValue();

        assertEquals(2029, saved.getBatchYear());
        assertEquals("2029_JJCET_08th_OCT_WEEKLY_ASSESSMENT", saved.getTestName());
        assertEquals("student2029@jjcet.ac.in", saved.getEmail());

        // Verify other physical tables were NOT written to
        verify(repo2027, never()).save(any());
        verify(repo2028, never()).save(any());
        verify(repo2030, never()).save(any());
    }

    // 1b. Test name variants for weekly assessments: WEEK, WEEKLY, A WEEK, A_WEEK
    @Test
    @DisplayName("Scenario 1b: Accepts week variants such as WEEK, WEEKLY, A WEEK, A_WEEK")
    void testWeeklyAssessmentVariantsAccepted() {
        List<String> validTestNames = List.of(
                "2029_JJCET_08th_OCT_WEEK_ASSESSMENT",
                "2029_JJCET_08th_OCT_WEEKLY_ASSESSMENT",
                "2029_JJCET_08th_OCT_A_WEEK_ASSESSMENT",
                "2029_JJCET_08th_OCT_A WEEK ASSESSMENT",
                "2029_JJCET_WEEK_1_ASSESSMENT",
                "2029_JJCET_WEEKLY",
                "2029_JJCET_WEEK"
        );

        for (String testName : validTestNames) {
            var result = NeopatBatchAssessmentFilter.evaluate(testName);
            assertTrue(result.accepted(), "Should accept: " + testName + " but got: " + result.reason());
            assertEquals(2029, result.batchYear());
        }
    }

    // 1c. Payload with title instead of test_name and arbitrary test_id like TEST-001
    @Test
    @DisplayName("Scenario 1c: Payload with title instead of test_name and arbitrary test_id like TEST-001 is accepted for batch 2029")
    void testPayloadWithTitleAndArbitraryTestIdAccepted() {
        String payloadJson = """
                {
                  "email": "student2029@jjcet.ac.in",
                  "title": "2029_JJCET_08th_OCT_WEEKLY_ASSESSMENT",
                  "test_id": "TEST-001",
                  "marks": 85.5,
                  "total_marks": 100.0
                }
                """;

        when(repo2029.save(any(Assessment2029.class))).thenAnswer(i -> i.getArgument(0));

        ResponseEntity<Map<String, Boolean>> response = inboundController.receiveAssessment(payloadJson);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(Boolean.TRUE, response.getBody().get("success"));

        ArgumentCaptor<Assessment2029> captor = ArgumentCaptor.forClass(Assessment2029.class);
        verify(repo2029).save(captor.capture());
        Assessment2029 saved = captor.getValue();

        assertEquals(2029, saved.getBatchYear());
        assertEquals("2029_JJCET_08th_OCT_WEEKLY_ASSESSMENT", saved.getTestName());
        assertEquals("TEST-001", saved.getTestId());
    }

    // 2. A valid weekly assessment for each supported batch (2027, 2028, 2029, 2030) stored in corresponding table
    @Test
    @DisplayName("Scenario 2: Valid weekly assessments for all supported batches stored in respective tables")
    void testValidWeeklyAssessmentsForAllSupportedBatches() {
        // 2027 -> repo2027
        when(repo2027.save(any(Assessment2027.class))).thenAnswer(i -> i.getArgument(0));
        String p27 = """
                {"email": "s27@jjcet.ac.in", "test_name": "2027_JJCET_WEEKLY_ASSESSMENT", "test_id": "T27", "marks": 80, "total_marks": 100}
                """;
        inboundController.receiveAssessment(p27);
        verify(repo2027).save(any(Assessment2027.class));

        // 2028 -> repo2028
        when(repo2028.save(any(Assessment2028.class))).thenAnswer(i -> i.getArgument(0));
        String p28 = """
                {"email": "s28@jjcet.ac.in", "test_name": "2028_JJCET_WEEKLY_ASSESSMENT", "test_id": "T28", "marks": 85, "total_marks": 100}
                """;
        inboundController.receiveAssessment(p28);
        verify(repo2028).save(any(Assessment2028.class));

        // 2029 -> repo2029
        when(repo2029.save(any(Assessment2029.class))).thenAnswer(i -> i.getArgument(0));
        String p29 = """
                {"email": "s29@jjcet.ac.in", "test_name": "2029_JJCET_WEEKLY_ASSESSMENT", "test_id": "T29", "marks": 90, "total_marks": 100}
                """;
        inboundController.receiveAssessment(p29);
        verify(repo2029).save(any(Assessment2029.class));

        // 2030 -> repo2030
        when(repo2030.save(any(Assessment2030.class))).thenAnswer(i -> i.getArgument(0));
        String p30 = """
                {"email": "s30@jjcet.ac.in", "test_name": "2030_JJCET_WEEKLY_ASSESSMENT", "test_id": "T30", "marks": 95, "total_marks": 100}
                """;
        inboundController.receiveAssessment(p30);
        verify(repo2030).save(any(Assessment2030.class));
    }

    // 3. A 2029 daily assessment being skipped
    @Test
    @DisplayName("Scenario 3: 2029 daily assessment is skipped, returns success: true, never stored in any table")
    void test2029DailyAssessmentSkipped() {
        String payloadJson = """
                {
                  "email": "student@jjcet.ac.in",
                  "test_name": "2029_JJCET_08th_OCT_DAILY_ASSESSMENT",
                  "test_id": "DAILY_TEST_01",
                  "marks": 40,
                  "total_marks": 50
                }
                """;

        ResponseEntity<Map<String, Boolean>> response = inboundController.receiveAssessment(payloadJson);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(Boolean.TRUE, response.getBody().get("success"));

        // Verify NOT stored in any batch physical table
        verify(repo2027, never()).save(any());
        verify(repo2028, never()).save(any());
        verify(repo2029, never()).save(any());
        verify(repo2030, never()).save(any());

        verify(auditLogService).logEvent(eq("INBOUND_ASSESSMENT"), anyString(), eq("ASSESSMENT_SKIPPED"), eq("SKIPPED"), contains("DAILY_ASSESSMENT_SKIPPED"), isNull());
    }

    // 4. An unsupported batch year being skipped
    @Test
    @DisplayName("Scenario 4: Unsupported batch year (e.g. 2026 or 2031) is skipped")
    void testUnsupportedBatchYearSkipped() {
        String payloadJson = """
                {
                  "email": "alumni@jjcet.ac.in",
                  "test_name": "2026_JJCET_08th_OCT_WEEKLY_ASSESSMENT",
                  "test_id": "WEEKLY_OLD",
                  "marks": 70,
                  "total_marks": 100
                }
                """;

        ResponseEntity<Map<String, Boolean>> response = inboundController.receiveAssessment(payloadJson);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(Boolean.TRUE, response.getBody().get("success"));

        verify(repo2027, never()).save(any());
        verify(repo2028, never()).save(any());
        verify(repo2029, never()).save(any());
        verify(repo2030, never()).save(any());

        verify(auditLogService).logEvent(eq("INBOUND_ASSESSMENT"), anyString(), eq("ASSESSMENT_SKIPPED"), eq("SKIPPED"), contains("UNSUPPORTED_BATCH_YEAR_2026"), isNull());
    }

    // 5. An invalid or malformed test name being skipped safely
    @Test
    @DisplayName("Scenario 5: Malformed test name without batch year or weekly marker is skipped safely")
    void testMalformedTestNameSkippedSafely() {
        String payloadJson = """
                {
                  "email": "student@jjcet.ac.in",
                  "test_name": "RANDOM_MIDTERM_MOCK_TEST",
                  "test_id": "TEST_RANDOM",
                  "marks": 50,
                  "total_marks": 100
                }
                """;

        ResponseEntity<Map<String, Boolean>> response = inboundController.receiveAssessment(payloadJson);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(Boolean.TRUE, response.getBody().get("success"));

        verify(repo2027, never()).save(any());
        verify(repo2028, never()).save(any());
        verify(repo2029, never()).save(any());
        verify(repo2030, never()).save(any());
    }

    // 6. Duplicate assessment submissions not creating duplicate records in batch table
    @Test
    @DisplayName("Scenario 6: Duplicate assessment updates existing record in batch table without duplicate row")
    void testDuplicateAssessmentUpdatesExistingRecord() {
        Assessment2028 existing = new Assessment2028();
        existing.setId(2877L);
        existing.setEmail("sharu@jjcet.ac.in");
        existing.setTestId("TEST_DUP_01");
        existing.setMarks(new BigDecimal("70.00"));
        existing.setTotalMarks(new BigDecimal("100.00"));

        when(repo2028.findByEmailAndTestId("sharu@jjcet.ac.in", "TEST_DUP_01"))
                .thenReturn(Optional.of(existing));
        when(repo2028.save(any(Assessment2028.class))).thenAnswer(i -> i.getArgument(0));

        String updatePayload = """
                {
                  "email": "sharu@jjcet.ac.in",
                  "test_name": "2028_JJCET_08th_OCT_WEEKLY_ASSESSMENT",
                  "test_id": "TEST_DUP_01",
                  "marks": 95.0,
                  "total_marks": 100.0
                }
                """;

        ResponseEntity<Map<String, Boolean>> response = inboundController.receiveAssessment(updatePayload);

        assertEquals(200, response.getStatusCode().value());
        ArgumentCaptor<Assessment2028> captor = ArgumentCaptor.forClass(Assessment2028.class);
        verify(repo2028).save(captor.capture());

        Assessment2028 saved = captor.getValue();
        assertEquals(2877L, saved.getId());
        assertEquals(new BigDecimal("95.0"), saved.getMarks());
    }

    // 7. Each batch having an independent schedule
    @Test
    @DisplayName("Scenario 7: Each batch has independent schedule configuration")
    void testEachBatchHasIndependentSchedule() {
        NeopatSmsSchedule sched2027 = new NeopatSmsSchedule(2027, "MONDAY", LocalTime.of(9, 0), true);
        NeopatSmsSchedule sched2028 = new NeopatSmsSchedule(2028, "TUESDAY", LocalTime.of(10, 0), true);
        NeopatSmsSchedule sched2029 = new NeopatSmsSchedule(2029, "WEDNESDAY", LocalTime.of(11, 0), true);
        NeopatSmsSchedule sched2030 = new NeopatSmsSchedule(2030, "THURSDAY", LocalTime.of(14, 0), false);

        when(scheduleRepository.findByBatchYear(2027)).thenReturn(Optional.of(sched2027));
        when(scheduleRepository.findByBatchYear(2028)).thenReturn(Optional.of(sched2028));
        when(scheduleRepository.findByBatchYear(2029)).thenReturn(Optional.of(sched2029));
        when(scheduleRepository.findByBatchYear(2030)).thenReturn(Optional.of(sched2030));

        NeopatBatchSmsConfigDto cfg2027 = orchestrationService.getBatchSettings(2027);
        NeopatBatchSmsConfigDto cfg2028 = orchestrationService.getBatchSettings(2028);
        NeopatBatchSmsConfigDto cfg2029 = orchestrationService.getBatchSettings(2029);
        NeopatBatchSmsConfigDto cfg2030 = orchestrationService.getBatchSettings(2030);

        assertEquals("MONDAY", cfg2027.getDayOfWeek());
        assertEquals("09:00", cfg2027.getSendTime());
        assertTrue(cfg2027.getEnabled());

        assertEquals("TUESDAY", cfg2028.getDayOfWeek());
        assertEquals("10:00", cfg2028.getSendTime());
        assertTrue(cfg2028.getEnabled());

        assertEquals("WEDNESDAY", cfg2029.getDayOfWeek());
        assertEquals("11:00", cfg2029.getSendTime());

        assertEquals("THURSDAY", cfg2030.getDayOfWeek());
        assertEquals("14:00", cfg2030.getSendTime());
        assertFalse(cfg2030.getEnabled());
    }

    // 8. Disabled schedules not triggering SMS
    @Test
    @DisplayName("Scenario 8: Disabled schedule does not trigger SMS")
    void testDisabledScheduleDoesNotTriggerSms() {
        NeopatSmsSchedule disabledSchedule = new NeopatSmsSchedule(2029, "SATURDAY", LocalTime.of(10, 0), false);
        when(scheduleRepository.findAllByOrderByBatchYearAsc()).thenReturn(List.of(disabledSchedule));

        scheduler.evaluateAndExecuteSchedule();

        verify(executionRepository, never()).save(any());
        verify(repo2029, never()).findByProcessingStatus(anyString());
        verify(twilioSmsService, never()).sendSms(anyString(), anyString());
    }

    // 9. Updating a schedule changing future execution timing
    @Test
    @DisplayName("Scenario 9: Updating schedule changes future execution timing independently")
    void testUpdatingScheduleChangesExecutionTiming() {
        NeopatSmsSchedule sched2028 = new NeopatSmsSchedule(2028, "MONDAY", LocalTime.of(9, 0), false);
        when(scheduleRepository.findByBatchYear(2028)).thenReturn(Optional.of(sched2028));
        when(scheduleRepository.saveAndFlush(any(NeopatSmsSchedule.class))).thenAnswer(i -> i.getArgument(0));

        NeopatSmsScheduleDto updateDto = new NeopatSmsScheduleDto(true, "FRIDAY", "16:45:00");
        NeopatBatchSmsConfigDto updated = orchestrationService.updateBatchSettings(2028, updateDto, "ADMIN_USER");

        assertTrue(updated.getEnabled());
        assertEquals("FRIDAY", updated.getDayOfWeek());
        assertEquals("16:45", updated.getSendTime());
        assertNotNull(updated.getNextExecution());
    }

    // 10. SMS processing selecting only eligible weekly assessments from the corresponding batch physical table
    @Test
    @DisplayName("Scenario 10: SMS processing selects only assessments from target batch physical table")
    void testSmsProcessingIsolatesTargetBatchTable() {
        Assessment2028 a2028 = new Assessment2028();
        a2028.setId(2801L);
        a2028.setEmail("student2028@jjcet.ac.in");
        a2028.setTestId("2028_JJCET_08th_OCT_WEEKLY_ASSESSMENT");
        a2028.setMarks(new BigDecimal("80"));
        a2028.setTotalMarks(new BigDecimal("100"));

        ParentContact contact = new ParentContact("student2028@jjcet.ac.in", "+919876543210");

        when(repo2028.findByProcessingStatus("PENDING")).thenReturn(List.of(a2028));
        when(parentContactRepository.findByStudentEmailAndIsActiveTrue("student2028@jjcet.ac.in"))
                .thenReturn(Optional.of(contact));
        when(twilioSmsService.sendSms(eq("+919876543210"), anyString()))
                .thenReturn(new NeopatTwilioSmsService.TwilioSendResult(true, "SID_2028", null));

        NeopatSmsOrchestrationService.BatchExecutionResult result =
                orchestrationService.processPendingAssessmentsBatch(2028);

        assertEquals(1, result.successCount);
        assertEquals(0, result.failureCount);

        // Verify other batch tables are NOT queried or deleted
        verify(repo2027, never()).findByProcessingStatus(anyString());
        verify(repo2029, never()).findByProcessingStatus(anyString());
        verify(repo2030, never()).findByProcessingStatus(anyString());

        // Verify deleted from assessment_2028 and archived to history
        verify(repo2028).delete(a2028);
        ArgumentCaptor<AssessmentHistory> histCaptor = ArgumentCaptor.forClass(AssessmentHistory.class);
        verify(assessmentHistoryRepository).save(histCaptor.capture());
        assertEquals(2028, histCaptor.getValue().getBatchYear());
    }

    // 11. Application restarts not causing duplicate SMS sends (idempotency)
    @Test
    @DisplayName("Scenario 11: Application restart does not duplicate SMS send if execution already recorded")
    void testApplicationRestartDoesNotDuplicateSms() {
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Asia/Kolkata"));
        LocalTime nowMinute = LocalTime.now(java.time.ZoneId.of("Asia/Kolkata")).withSecond(0).withNano(0);
        String currentDayName = java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Kolkata")).getDayOfWeek().name();

        NeopatSmsSchedule activeSchedule = new NeopatSmsSchedule(2029, currentDayName, nowMinute, true);
        when(scheduleRepository.findAllByOrderByBatchYearAsc()).thenReturn(List.of(activeSchedule));

        // Simulate execution already completed before restart
        when(executionRepository.existsByBatchYearAndScheduledDateAndScheduledTime(2029, today, nowMinute))
                .thenReturn(true);

        scheduler.evaluateAndExecuteSchedule();

        // Verify no second execution or SMS dispatch was triggered
        verify(executionRepository, never()).save(any());
        verify(repo2029, never()).findByProcessingStatus(anyString());
        verify(twilioSmsService, never()).sendSms(anyString(), anyString());
    }

    // 12. Authorization checks for schedule changes and manual triggers
    @Test
    @DisplayName("Scenario 12: Security authorization check for batch schedule endpoints")
    void testAuthorizationAnnotationsPresentOnBatchEndpoints() throws Exception {
        Method updateMethod = NeopatSmsSettingsController.class.getMethod("updateBatchSettings", Integer.class, NeopatSmsScheduleDto.class);
        PreAuthorize updateAuth = updateMethod.getAnnotation(PreAuthorize.class);
        assertNotNull(updateAuth, "updateBatchSettings must have @PreAuthorize");
        assertTrue(updateAuth.value().contains("SUPER_ADMIN") || updateAuth.value().contains("SUPERADMIN"));

        Method triggerMethod = NeopatSmsSettingsController.class.getMethod("triggerManualBatchRun", Integer.class);
        PreAuthorize triggerAuth = triggerMethod.getAnnotation(PreAuthorize.class);
        assertNotNull(triggerAuth, "triggerManualBatchRun must have @PreAuthorize");
        assertTrue(triggerAuth.value().contains("SUPER_ADMIN") || triggerAuth.value().contains("SUPERADMIN"));
    }

    // 13. Enabling Batch 2027 and saving it does not modify Batch 2028, 2029, or 2030
    @Test
    @DisplayName("Scenario 13: Enabling Batch 2027 does not enable, modify, or overwrite Batches 2028, 2029, or 2030")
    void testEnablingBatch2027DoesNotAffectOtherBatches() {
        NeopatSmsSchedule sched2027 = new NeopatSmsSchedule(2027, "SATURDAY", LocalTime.of(10, 0), false);
        NeopatSmsSchedule sched2028 = new NeopatSmsSchedule(2028, "SATURDAY", LocalTime.of(10, 0), false);
        NeopatSmsSchedule sched2029 = new NeopatSmsSchedule(2029, "SATURDAY", LocalTime.of(10, 0), false);
        NeopatSmsSchedule sched2030 = new NeopatSmsSchedule(2030, "SATURDAY", LocalTime.of(10, 0), false);

        when(scheduleRepository.findByBatchYear(2027)).thenReturn(Optional.of(sched2027));
        when(scheduleRepository.findByBatchYear(2028)).thenReturn(Optional.of(sched2028));
        when(scheduleRepository.findByBatchYear(2029)).thenReturn(Optional.of(sched2029));
        when(scheduleRepository.findByBatchYear(2030)).thenReturn(Optional.of(sched2030));
        when(scheduleRepository.saveAndFlush(any(NeopatSmsSchedule.class))).thenAnswer(i -> i.getArgument(0));

        // Super Admin enables Batch 2027 with Friday 18:00
        NeopatSmsScheduleDto update2027 = new NeopatSmsScheduleDto(true, "FRIDAY", "18:00:00");
        NeopatBatchSmsConfigDto updated2027 = orchestrationService.updateBatchSettings(2027, update2027, "SUPER_ADMIN");

        // Verify Batch 2027 is now enabled
        assertTrue(updated2027.getEnabled(), "Batch 2027 must be enabled");
        assertEquals("FRIDAY", updated2027.getDayOfWeek());
        assertEquals("18:00", updated2027.getSendTime());

        // Verify other batches remained disabled and untouched
        NeopatBatchSmsConfigDto check2028 = orchestrationService.getBatchSettings(2028);
        assertFalse(check2028.getEnabled(), "Batch 2028 must remain disabled");
        assertEquals("SATURDAY", check2028.getDayOfWeek());
        assertEquals("10:00", check2028.getSendTime());

        NeopatBatchSmsConfigDto check2029 = orchestrationService.getBatchSettings(2029);
        assertFalse(check2029.getEnabled(), "Batch 2029 must remain disabled");

        NeopatBatchSmsConfigDto check2030 = orchestrationService.getBatchSettings(2030);
        assertFalse(check2030.getEnabled(), "Batch 2030 must remain disabled");

        // Verify saveAndFlush was called strictly for Batch 2027
        ArgumentCaptor<NeopatSmsSchedule> captor = ArgumentCaptor.forClass(NeopatSmsSchedule.class);
        verify(scheduleRepository).saveAndFlush(captor.capture());
        assertEquals(2027, captor.getValue().getBatchYear());
        assertTrue(captor.getValue().getEnabled());
    }
}
