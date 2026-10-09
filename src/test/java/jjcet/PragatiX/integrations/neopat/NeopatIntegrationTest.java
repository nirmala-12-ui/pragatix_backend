package jjcet.PragatiX.integrations.neopat;

import com.fasterxml.jackson.databind.ObjectMapper;
import jjcet.PragatiX.entity.Student;
import jjcet.PragatiX.integrations.neopat.config.TwilioConfigProperties;
import jjcet.PragatiX.integrations.neopat.controller.NeopatInboundAssessmentController;
import jjcet.PragatiX.integrations.neopat.dto.NeopatSmsScheduleDto;
import jjcet.PragatiX.integrations.neopat.dto.NeopatSmsSettingsResponseDto;
import jjcet.PragatiX.integrations.neopat.entity.*;
import jjcet.PragatiX.integrations.neopat.repository.*;
import jjcet.PragatiX.integrations.neopat.security.NeopatApiKeyFilter;
import jjcet.PragatiX.integrations.neopat.service.*;
import jjcet.PragatiX.modules.student.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NeopatIntegrationTest {

    @Mock
    private CurrentAssessmentRepository currentAssessmentRepository;

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

    private TwilioConfigProperties twilioConfig;
    private NeopatSmsOrchestrationService orchestrationService;
    private NeopatAssessmentProcessingService processingService;
    private NeopatInboundAssessmentController controller;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        twilioConfig = new TwilioConfigProperties();
        twilioConfig.setTestPhoneNumber("+919876543210");
        twilioConfig.setFromNumber("+1234567890");

        orchestrationService = new NeopatSmsOrchestrationService(
                currentAssessmentRepository,
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

        processingService = new NeopatAssessmentProcessingService(
                currentAssessmentRepository,
                failedAssessmentRepository,
                auditLogService,
                objectMapper
        );

        controller = new NeopatInboundAssessmentController(processingService, objectMapper);
    }

    @Test
    @DisplayName("API Key Filter: rejects requests with missing or invalid x-apikey header returning strictly {\"success\": false}")
    void testApiKeyFilterUnauthorized() throws Exception {
        NeopatApiKeyFilter filter = new NeopatApiKeyFilter("valid-secret-key-123", objectMapper);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/integrations/neopat/assessment");
        request.addHeader("x-apikey", "wrong-key");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        assertEquals(401, response.getStatus());
        assertEquals("{\"success\":false}", response.getContentAsString().replaceAll("\\s+", ""));
    }

    @Test
    @DisplayName("API Key Filter: allows requests with matching x-apikey header")
    void testApiKeyFilterAuthorized() throws Exception {
        NeopatApiKeyFilter filter = new NeopatApiKeyFilter("valid-secret-key-123", objectMapper);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/integrations/neopat/assessment");
        request.addHeader("x-apikey", "valid-secret-key-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        assertEquals(200, response.getStatus());
    }

    @Test
    @DisplayName("Controller: Valid payload with multiple sections stores raw_payload and returns strictly {\"success\": true}")
    void testInboundAssessmentWithSections() {
        String jsonPayload = """
                {
                  "email": "student@jjcet.ac.in",
                  "test_id": "TEST_FULL_01",
                  "marks": "92.50",
                  "total_marks": "100.00",
                  "attempts": 1,
                  "result_analysis_url": "https://neopat.ai/report/xyz",
                  "starttime": "2026-10-09 03:30:00",
                  "submittime": "2026-10-09 04:30:00",
                  "custom_extra_field_2026": "preserve_this_completely",
                  "section_wise_marks": [
                    {
                      "name": "Quantitative",
                      "marks": "45.00",
                      "total_marks": "50.00",
                      "cut_off_marks": "30.00",
                      "section_result": "PASS"
                    },
                    {
                      "name": "Verbal",
                      "marks": "47.50",
                      "total_marks": "50.00"
                    }
                  ]
                }
                """;

        when(currentAssessmentRepository.save(any(CurrentAssessment.class))).thenAnswer(i -> {
            CurrentAssessment ca = i.getArgument(0);
            ca.setId(501L);
            return ca;
        });

        ResponseEntity<Map<String, Boolean>> response = controller.receiveAssessment(jsonPayload);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size(), "Response body must contain exactly 1 field");
        assertEquals(Boolean.TRUE, response.getBody().get("success"));

        ArgumentCaptor<CurrentAssessment> captor = ArgumentCaptor.forClass(CurrentAssessment.class);
        verify(currentAssessmentRepository).save(captor.capture());
        CurrentAssessment saved = captor.getValue();

        assertEquals("student@jjcet.ac.in", saved.getEmail());
        assertEquals("TEST_FULL_01", saved.getTestId());
        assertEquals(new BigDecimal("92.50"), saved.getMarks());
        // Verify start_time and submit_time remain in UTC as sent
        assertEquals(LocalDateTime.of(2026, 10, 9, 3, 30, 0), saved.getStartTime());
        assertEquals(LocalDateTime.of(2026, 10, 9, 4, 30, 0), saved.getSubmitTime());
        // Verify raw_payload contains custom unknown fields
        assertTrue(saved.getRawPayload().contains("custom_extra_field_2026"));
        // Verify section_wise_marks is persisted
        assertTrue(saved.getSectionWiseMarks().contains("Quantitative"));
        // Verify IST timestamps for system records
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getReceivedAt());
    }

    @Test
    @DisplayName("Controller: Valid payload with NO sections and absent starttime returns strictly {\"success\": true}")
    void testInboundAssessmentWithNoSectionsAndNoStartTime() {
        String jsonPayload = """
                {
                  "email": "student2@jjcet.ac.in",
                  "test_id": "TEST_NO_SEC",
                  "marks": "75.00",
                  "total_marks": "100.00",
                  "attempts": 2,
                  "submittime": "2026-10-09 05:00:00",
                  "section_wise_marks": []
                }
                """;

        when(currentAssessmentRepository.save(any(CurrentAssessment.class))).thenAnswer(i -> {
            CurrentAssessment ca = i.getArgument(0);
            ca.setId(502L);
            return ca;
        });

        ResponseEntity<Map<String, Boolean>> response = controller.receiveAssessment(jsonPayload);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(Map.of("success", true), response.getBody());

        ArgumentCaptor<CurrentAssessment> captor = ArgumentCaptor.forClass(CurrentAssessment.class);
        verify(currentAssessmentRepository).save(captor.capture());
        assertNull(captor.getValue().getStartTime());
        assertEquals("[]", captor.getValue().getSectionWiseMarks().trim());
    }

    @Test
    @DisplayName("Controller: Malformed JSON is retained in failed_assessment without data loss and returns strictly {\"success\": false}")
    void testMalformedJsonRecordedWithoutDataLoss() {
        String brokenJson = "{\"email\": \"invalid-json-content...";

        ResponseEntity<Map<String, Boolean>> response = controller.receiveAssessment(brokenJson);

        assertEquals(400, response.getStatusCode().value());
        assertEquals(Map.of("success", false), response.getBody());

        ArgumentCaptor<FailedAssessment> captor = ArgumentCaptor.forClass(FailedAssessment.class);
        verify(failedAssessmentRepository).save(captor.capture());
        assertEquals("MALFORMED_JSON", captor.getValue().getFailureType());
        assertEquals(brokenJson, captor.getValue().getRawPayload());
    }

    @Test
    @DisplayName("Cross-DB Validation: Records failure if student is not found in PragatiX DB")
    void testStudentNotFoundMovesToFailedAssessment() {
        CurrentAssessment assessment = new CurrentAssessment();
        assessment.setId(101L);
        assessment.setEmail("unknown@jjcet.ac.in");
        assessment.setTestId("NEO_JAVA_01");
        assessment.setMarks(new BigDecimal("85.00"));
        assessment.setTotalMarks(new BigDecimal("100.00"));
        assessment.setRawPayload("{\"email\":\"unknown@jjcet.ac.in\"}");

        when(currentAssessmentRepository.findByProcessingStatus("PENDING"))
                .thenReturn(List.of(assessment));
        when(studentRepository.findByEmail("unknown@jjcet.ac.in")).thenReturn(Optional.empty());

        NeopatSmsOrchestrationService.BatchExecutionResult result = orchestrationService.processPendingAssessmentsBatch();

        assertEquals(0, result.successCount);
        assertEquals(1, result.failureCount);

        ArgumentCaptor<FailedAssessment> failCaptor = ArgumentCaptor.forClass(FailedAssessment.class);
        verify(failedAssessmentRepository).save(failCaptor.capture());
        assertEquals("STUDENT_NOT_FOUND", failCaptor.getValue().getFailureType());
        assertEquals(assessment.getRawPayload(), failCaptor.getValue().getRawPayload());
        verify(currentAssessmentRepository).delete(assessment);
    }

    @Test
    @DisplayName("Cross-DB Validation: Records failure if parent contact is not found in neopa_sms")
    void testParentContactNotFoundMovesToFailedAssessment() {
        CurrentAssessment assessment = new CurrentAssessment();
        assessment.setId(102L);
        assessment.setEmail("student@jjcet.ac.in");
        assessment.setTestId("NEO_PYTHON_01");
        assessment.setMarks(new BigDecimal("90.00"));
        assessment.setTotalMarks(new BigDecimal("100.00"));
        assessment.setRawPayload("{\"email\":\"student@jjcet.ac.in\"}");

        Student student = new Student();
        student.setEmail("student@jjcet.ac.in");
        student.setFullName("John Doe");

        when(currentAssessmentRepository.findByProcessingStatus("PENDING"))
                .thenReturn(List.of(assessment));
        when(studentRepository.findByEmail("student@jjcet.ac.in")).thenReturn(Optional.of(student));
        when(parentContactRepository.findByStudentEmailAndIsActiveTrue("student@jjcet.ac.in"))
                .thenReturn(Optional.empty());

        NeopatSmsOrchestrationService.BatchExecutionResult result = orchestrationService.processPendingAssessmentsBatch();

        assertEquals(0, result.successCount);
        assertEquals(1, result.failureCount);

        ArgumentCaptor<FailedAssessment> failCaptor = ArgumentCaptor.forClass(FailedAssessment.class);
        verify(failedAssessmentRepository).save(failCaptor.capture());
        assertEquals("PARENT_CONTACT_NOT_FOUND", failCaptor.getValue().getFailureType());
        assertEquals(assessment.getRawPayload(), failCaptor.getValue().getRawPayload());
        verify(currentAssessmentRepository).delete(assessment);
    }

    @Test
    @DisplayName("CRITICAL SMS FAILURE RULE: Twilio failure still archives to assessment_history with sms_status = FAILED (NOT failed_assessment)")
    void testTwilioFailureArchivesToHistoryAsFailed() {
        CurrentAssessment assessment = new CurrentAssessment();
        assessment.setId(103L);
        assessment.setEmail("student@jjcet.ac.in");
        assessment.setTestId("NEO_CPP_01");
        assessment.setMarks(new BigDecimal("75.00"));
        assessment.setTotalMarks(new BigDecimal("100.00"));
        assessment.setRawPayload("{\"email\":\"student@jjcet.ac.in\",\"raw\":true}");

        Student student = new Student();
        student.setEmail("student@jjcet.ac.in");
        student.setFullName("Jane Doe");

        ParentContact parent = new ParentContact("student@jjcet.ac.in", "+919876543210");

        when(currentAssessmentRepository.findByProcessingStatus("PENDING"))
                .thenReturn(List.of(assessment));
        when(studentRepository.findByEmail("student@jjcet.ac.in")).thenReturn(Optional.of(student));
        when(parentContactRepository.findByStudentEmailAndIsActiveTrue("student@jjcet.ac.in"))
                .thenReturn(Optional.of(parent));

        when(twilioSmsService.sendSms(eq("+919876543210"), anyString()))
                .thenReturn(new NeopatTwilioSmsService.TwilioSendResult(false, null, "Twilio timeout error"));

        NeopatSmsOrchestrationService.BatchExecutionResult result = orchestrationService.processPendingAssessmentsBatch();

        assertEquals(0, result.successCount);
        assertEquals(1, result.failureCount);

        // Verify NOT saved into failed_assessment!
        verify(failedAssessmentRepository, never()).save(any());

        // Verify saved into assessment_history with sms_status = FAILED and preserved raw_payload
        ArgumentCaptor<AssessmentHistory> histCaptor = ArgumentCaptor.forClass(AssessmentHistory.class);
        verify(assessmentHistoryRepository).save(histCaptor.capture());
        assertEquals("FAILED", histCaptor.getValue().getSmsStatus());
        assertEquals("Twilio timeout error", histCaptor.getValue().getSmsFailureReason());
        assertEquals(assessment.getRawPayload(), histCaptor.getValue().getRawPayload());
        verify(currentAssessmentRepository).delete(assessment);
    }

    @Test
    @DisplayName("Successful Flow: Twilio success archives to assessment_history with sms_status = SENT and removes from current")
    void testTwilioSuccessArchivesToHistoryAsSent() {
        CurrentAssessment assessment = new CurrentAssessment();
        assessment.setId(104L);
        assessment.setEmail("student@jjcet.ac.in");
        assessment.setTestId("NEO_DSA_01");
        assessment.setMarks(new BigDecimal("95.00"));
        assessment.setTotalMarks(new BigDecimal("100.00"));
        assessment.setRawPayload("{\"email\":\"student@jjcet.ac.in\",\"test_id\":\"NEO_DSA_01\"}");

        Student student = new Student();
        student.setEmail("student@jjcet.ac.in");
        student.setFullName("Alice Smith");

        ParentContact parent = new ParentContact("student@jjcet.ac.in", "+919123456789");

        when(currentAssessmentRepository.findByProcessingStatus("PENDING"))
                .thenReturn(List.of(assessment));
        when(studentRepository.findByEmail("student@jjcet.ac.in")).thenReturn(Optional.of(student));
        when(parentContactRepository.findByStudentEmailAndIsActiveTrue("student@jjcet.ac.in"))
                .thenReturn(Optional.of(parent));

        when(twilioSmsService.sendSms(eq("+919123456789"), anyString()))
                .thenReturn(new NeopatTwilioSmsService.TwilioSendResult(true, "SM_TEST_123", null));

        NeopatSmsOrchestrationService.BatchExecutionResult result = orchestrationService.processPendingAssessmentsBatch();

        assertEquals(1, result.successCount);
        assertEquals(0, result.failureCount);

        ArgumentCaptor<AssessmentHistory> histCaptor = ArgumentCaptor.forClass(AssessmentHistory.class);
        verify(assessmentHistoryRepository).save(histCaptor.capture());
        assertEquals("SENT", histCaptor.getValue().getSmsStatus());
        assertNotNull(histCaptor.getValue().getSmsSentAt());
        assertEquals(assessment.getRawPayload(), histCaptor.getValue().getRawPayload());
        verify(currentAssessmentRepository).delete(assessment);
    }

    @Test
    @DisplayName("Schedule Management: Update weekly schedule settings")
    void testScheduleSettingsUpdate() {
        NeopatSmsSchedule schedule = new NeopatSmsSchedule();
        schedule.setId(1L);
        schedule.setDayOfWeek("SATURDAY");
        schedule.setSendTime(LocalTime.of(10, 0));
        schedule.setEnabled(false);

        when(scheduleRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(schedule));
        when(scheduleRepository.save(any(NeopatSmsSchedule.class))).thenAnswer(i -> i.getArgument(0));

        NeopatSmsScheduleDto dto = new NeopatSmsScheduleDto(true, "SUNDAY", "11:30:00");
        NeopatSmsSettingsResponseDto res = orchestrationService.updateSettings(dto, "SUPERADMIN_USER");

        assertTrue(res.getEnabled());
        assertEquals("SUNDAY", res.getDayOfWeek());
        assertEquals("11:30", res.getSendTime());
        assertEquals("SUPERADMIN_USER", res.getUpdatedBy());
    }
}
