package jjcet.PragatiX.modules.student.service;

import jjcet.PragatiX.common.response.ApiResponse;
import jjcet.PragatiX.dto.AwardXpRequest;
import jjcet.PragatiX.entity.*;
import jjcet.PragatiX.modules.activity.repository.ActivityRepository;
import jjcet.PragatiX.modules.activity.repository.ActivityStageRepository;
import jjcet.PragatiX.modules.activity.service.ActivityStreakService;
import jjcet.PragatiX.modules.activity.service.AssignmentSecurityService;
import jjcet.PragatiX.modules.activity.service.StageValidationService;
import jjcet.PragatiX.modules.admin.service.ActivityRequestMapper;
import jjcet.PragatiX.modules.audit.service.AuditService;
import jjcet.PragatiX.modules.authentication.repository.UserRepository;
import jjcet.PragatiX.modules.student.repository.StudentActivityXpRepository;
import jjcet.PragatiX.modules.student.repository.StudentRepository;
import jjcet.PragatiX.repository.ActivityAssignmentRepository;
import jjcet.PragatiX.repository.PenaltyRequestRepository;
import jjcet.PragatiX.repository.StreakRepository;
import jjcet.PragatiX.repository.XpTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class VariableXpServiceTest {

    @Mock
    private StudentRepository studentRepository;
    @Mock
    private StudentActivityXpRepository studentActivityXpRepository;
    @Mock
    private XpTransactionRepository xpTransactionRepository;
    @Mock
    private ActivityStageRepository activityStageRepository;
    @Mock
    private StageValidationService stageValidationService;
    @Mock
    private TeamAssignmentService teamAssignmentService;
    @Mock
    private jjcet.PragatiX.admin.service.CaptainSelectionService captainSelectionService;
    @Mock
    private StreakRepository streakRepository;
    @Mock
    private ActivityStreakService activityStreakService;
    @Mock
    private AuditService auditService;

    @Mock
    private UserRepository userRepository;
    @Mock
    private ActivityRepository activityRepository;
    @Mock
    private ActivityAssignmentRepository activityAssignmentRepository;
    @Mock
    private AssignmentSecurityService assignmentSecurityService;
    @Mock
    private StudentXpValidator validator;
    @Mock
    private PenaltyRequestRepository penaltyRequestRepository;

    private XpEngineService xpEngineService;
    private StudentXpService studentXpService;

    private User teacherUser;

    @BeforeEach
    void setUp() {
        xpEngineService = new XpEngineService(
                studentRepository,
                studentActivityXpRepository,
                xpTransactionRepository,
                activityStageRepository,
                stageValidationService,
                teamAssignmentService,
                captainSelectionService,
                streakRepository,
                activityStreakService,
                auditService
        );

        studentXpService = new StudentXpService(
                userRepository,
                activityRepository,
                activityAssignmentRepository,
                studentRepository,
                assignmentSecurityService,
                validator,
                xpEngineService,
                penaltyRequestRepository
        );

        teacherUser = new User();
        teacherUser.setId(1L);
        teacherUser.setUsername("teacher1");
        teacherUser.setFullName("Prof. Sharma");

        when(xpTransactionRepository.saveAndFlush(any(XpTransaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(studentRepository.save(any(Student.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("Activity entity awardType classification: Variable XP vs Fixed XP")
    void testActivityEntityClassification() {
        Activity varAct1 = new Activity();
        varAct1.setAwardType("Variable XP");
        assertTrue(varAct1.isVariableXp());
        assertFalse(varAct1.isFixedXp());

        Activity varAct2 = new Activity();
        varAct2.setAwardType("Variable XP (future use)");
        assertTrue(varAct2.isVariableXp());
        assertFalse(varAct2.isFixedXp());

        Activity varAct3 = new Activity();
        varAct3.setAwardType("VARIABLE");
        assertTrue(varAct3.isVariableXp());
        assertFalse(varAct3.isFixedXp());

        Activity fixedAct = new Activity();
        fixedAct.setAwardType("Fixed XP");
        assertFalse(fixedAct.isVariableXp());
        assertTrue(fixedAct.isFixedXp());

        Activity legacyNull = new Activity();
        legacyNull.setAwardType(null);
        assertFalse(legacyNull.isVariableXp());
        assertTrue(legacyNull.isFixedXp());

        Activity legacyBlank = new Activity();
        legacyBlank.setAwardType("   ");
        assertFalse(legacyBlank.isVariableXp());
        assertTrue(legacyBlank.isFixedXp());
    }

    @Test
    @DisplayName("ActivityRequestMapper properly normalizes awardType")
    void testActivityRequestMapperNormalization() {
        ActivityRequestMapper mapper = new ActivityRequestMapper();

        Map<String, Object> body1 = new HashMap<>();
        body1.put("awardType", "Variable XP (future use)");
        assertEquals("Variable XP", mapper.parseAwardType(body1));

        Map<String, Object> body2 = new HashMap<>();
        body2.put("awardType", "VARIABLE");
        assertEquals("Variable XP", mapper.parseAwardType(body2));

        Map<String, Object> body3 = new HashMap<>();
        body3.put("awardType", null);
        assertEquals("Fixed XP", mapper.parseAwardType(body3));
    }

    @Test
    @DisplayName("Fixed XP Activity: Always awards configured XP, ignoring arbitrary request XP")
    void testFixedXp_AlwaysAwardsConfiguredAmount() {
        Student student = new Student();
        student.setId(10L);
        student.setRegNo("REG001");
        student.setTotalXp(100);
        student.setScore(100);
        student.setStage(1);

        Activity fixedActivity = new Activity();
        fixedActivity.setId(1L);
        fixedActivity.setName("Fixed Assignment");
        fixedActivity.setAwardType("Fixed XP");
        fixedActivity.setAwardXp(50);
        fixedActivity.setAwardEnabled(true);
        fixedActivity.setPenaltyEnabled(false);

        // Calling xpEngineService with requestXp = 90
        Student updated = xpEngineService.awardXp(student, fixedActivity, teacherUser, null, 90, "Testing fixed award");

        // Verify Student total XP increased by configured 50 (100 -> 150)
        assertEquals(150, updated.getTotalXp());

        // Verify transaction saved with exactly 50 XP
        ArgumentCaptor<XpTransaction> txCaptor = ArgumentCaptor.forClass(XpTransaction.class);
        verify(xpTransactionRepository).saveAndFlush(txCaptor.capture());
        assertEquals(50, txCaptor.getValue().getXpPoints());
    }

    @Test
    @DisplayName("Variable XP Activity: Awards exact performance amount to multiple students (90, 75, 40)")
    void testVariableXp_AwardsDynamicPerformanceAmounts() {
        Activity varActivity = new Activity();
        varActivity.setId(2L);
        varActivity.setName("Hackathon Project");
        varActivity.setAwardType("Variable XP");
        varActivity.setAwardXp(50); // baseline default
        varActivity.setAwardEnabled(true);
        varActivity.setPenaltyEnabled(false);

        // Student A -> 90 XP
        Student studentA = new Student();
        studentA.setId(101L);
        studentA.setRegNo("STU_A");
        studentA.setTotalXp(0);
        studentA.setScore(0);
        studentA.setStage(1);

        Student resA = xpEngineService.awardXp(studentA, varActivity, teacherUser, null, 90, "Excellent submission");
        assertEquals(90, resA.getTotalXp());

        // Student B -> 75 XP
        Student studentB = new Student();
        studentB.setId(102L);
        studentB.setRegNo("STU_B");
        studentB.setTotalXp(200);
        studentB.setScore(200);
        studentB.setStage(1);

        Student resB = xpEngineService.awardXp(studentB, varActivity, teacherUser, null, 75, "Very good submission");
        assertEquals(275, resB.getTotalXp());

        // Student C -> 40 XP
        Student studentC = new Student();
        studentC.setId(103L);
        studentC.setRegNo("STU_C");
        studentC.setTotalXp(50);
        studentC.setScore(50);
        studentC.setStage(1);

        Student resC = xpEngineService.awardXp(studentC, varActivity, teacherUser, null, 40, "Satisfactory submission");
        assertEquals(90, resC.getTotalXp());

        // Verify each transaction stored exact amount
        ArgumentCaptor<XpTransaction> txCaptor = ArgumentCaptor.forClass(XpTransaction.class);
        verify(xpTransactionRepository, times(3)).saveAndFlush(txCaptor.capture());
        List<XpTransaction> transactions = txCaptor.getAllValues();
        assertEquals(90, transactions.get(0).getXpPoints());
        assertEquals(75, transactions.get(1).getXpPoints());
        assertEquals(40, transactions.get(2).getXpPoints());
    }

    @Test
    @DisplayName("Legacy Activity without awardType behaves strictly as Fixed XP (backward compatibility)")
    void testLegacyActivity_NullAwardType_BehavesAsFixedXp() {
        Student student = new Student();
        student.setId(10L);
        student.setRegNo("REG001");
        student.setTotalXp(0);
        student.setScore(0);
        student.setStage(1);

        Activity legacyActivity = new Activity();
        legacyActivity.setId(99L);
        legacyActivity.setName("Legacy Assignment");
        legacyActivity.setAwardType(null); // Legacy activity created before Variable XP
        legacyActivity.setAwardXp(50);
        legacyActivity.setAwardEnabled(true);
        legacyActivity.setPenaltyEnabled(false);

        // If a request provides 85 XP, legacy activity must stick to configured 50 XP
        Student updated = xpEngineService.awardXp(student, legacyActivity, teacherUser, null, 85, "Legacy award");
        assertEquals(50, updated.getTotalXp());

        ArgumentCaptor<XpTransaction> txCaptor = ArgumentCaptor.forClass(XpTransaction.class);
        verify(xpTransactionRepository).saveAndFlush(txCaptor.capture());
        assertEquals(50, txCaptor.getValue().getXpPoints());
    }

    @Test
    @DisplayName("Validation: Variable XP activity rejects zero or missing XP amount")
    void testStudentXpService_Validation_VariableXpRequiresPositiveXp() {
        ActivitySubgroup subgroup = new ActivitySubgroup();
        subgroup.setName("Core");

        Activity varActivity = new Activity();
        varActivity.setId(2L);
        varActivity.setName("Variable Assignment");
        varActivity.setAwardType("Variable XP");
        varActivity.setAwardXp(50);
        varActivity.setAwardEnabled(true);
        varActivity.setSubgroup(subgroup);

        Student student = new Student();
        student.setId(101L);
        student.setStage(1);

        ActivityAssignment assignment = new ActivityAssignment();
        assignment.setId(500L);
        assignment.setAssignmentScope(AssignmentScope.GLOBAL);
        assignment.setActivity(varActivity);

        when(userRepository.findByUsername("teacher1")).thenReturn(Optional.of(teacherUser));
        when(studentRepository.findById(101L)).thenReturn(Optional.of(student));
        when(activityRepository.findById(2L)).thenReturn(Optional.of(varActivity));
        when(activityAssignmentRepository.findByActivityId(2L)).thenReturn(List.of(assignment));
        when(assignmentSecurityService.isUserAssignedFaculty(any(), any())).thenReturn(true);

        AwardXpRequest invalidReq = new AwardXpRequest();
        invalidReq.setRegNo(101L);
        invalidReq.setActivityId(2L);
        invalidReq.setAssignmentId(500L);
        invalidReq.setXp(0); // Invalid for variable XP

        ResponseEntity<ApiResponse<Void>> response = studentXpService.awardStudentXp(invalidReq, "teacher1");
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().getMessage().contains("Award XP value must be greater than zero for Variable XP activity."));
    }

    @Test
    @DisplayName("Batch Awarding: Variable XP Activity with studentXpMap awards distinct amounts to each student")
    void testStudentXpService_BatchAward_VariableXp_StudentXpMap() {
        ActivitySubgroup subgroup = new ActivitySubgroup();
        subgroup.setName("Core");

        Activity varActivity = new Activity();
        varActivity.setId(2L);
        varActivity.setName("Project Review");
        varActivity.setAwardType("Variable XP");
        varActivity.setAwardXp(50);
        varActivity.setAwardEnabled(true);
        varActivity.setSubgroup(subgroup);

        Student studentA = new Student();
        studentA.setId(101L);
        studentA.setFullName("Student A");
        studentA.setRegNo("STU_A");
        studentA.setStage(1);

        Student studentB = new Student();
        studentB.setId(102L);
        studentB.setFullName("Student B");
        studentB.setRegNo("STU_B");
        studentB.setStage(1);

        Student studentC = new Student();
        studentC.setId(103L);
        studentC.setFullName("Student C");
        studentC.setRegNo("STU_C");
        studentC.setStage(1);

        ActivityAssignment assignment = new ActivityAssignment();
        assignment.setId(500L);
        assignment.setAssignmentScope(AssignmentScope.GLOBAL);
        assignment.setActivity(varActivity);

        when(userRepository.findByUsername("teacher1")).thenReturn(Optional.of(teacherUser));
        when(activityRepository.findById(2L)).thenReturn(Optional.of(varActivity));
        when(activityAssignmentRepository.findById(500L)).thenReturn(Optional.of(assignment));
        when(activityAssignmentRepository.findByActivityId(2L)).thenReturn(List.of(assignment));
        when(assignmentSecurityService.isUserAssignedFaculty(any(), any())).thenReturn(true);
        when(studentRepository.findAllById(anyCollection())).thenReturn(List.of(studentA, studentB, studentC));
        when(studentRepository.findById(101L)).thenReturn(Optional.of(studentA));
        when(studentRepository.findById(102L)).thenReturn(Optional.of(studentB));
        when(studentRepository.findById(103L)).thenReturn(Optional.of(studentC));

        AwardXpRequest batchReq = new AwardXpRequest();
        batchReq.setActivityId(2L);
        batchReq.setAssignmentId(500L);
        batchReq.setStudentIds(List.of(101L, 102L, 103L));
        Map<Long, Integer> studentXpMap = new HashMap<>();
        studentXpMap.put(101L, 90);
        studentXpMap.put(102L, 75);
        studentXpMap.put(103L, 40);
        batchReq.setStudentXpMap(studentXpMap);

        ResponseEntity<?> response = studentXpService.awardStudentXpBatch(batchReq, "teacher1");
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // Verify transaction savings
        ArgumentCaptor<XpTransaction> txCaptor = ArgumentCaptor.forClass(XpTransaction.class);
        verify(xpTransactionRepository, times(3)).saveAndFlush(txCaptor.capture());
        List<XpTransaction> savedTxs = txCaptor.getAllValues();
        assertEquals(90, savedTxs.get(0).getXpPoints());
        assertEquals(75, savedTxs.get(1).getXpPoints());
        assertEquals(40, savedTxs.get(2).getXpPoints());
    }

    @Test
    @DisplayName("Batch Awarding: Fixed XP Activity strictly enforces configured XP even if studentXpMap is sent")
    void testStudentXpService_BatchAward_FixedXp_EnforcesConfiguredAmount() {
        ActivitySubgroup subgroup = new ActivitySubgroup();
        subgroup.setName("Core");

        Activity fixedActivity = new Activity();
        fixedActivity.setId(1L);
        fixedActivity.setName("Fixed Quiz");
        fixedActivity.setAwardType("Fixed XP");
        fixedActivity.setAwardXp(50);
        fixedActivity.setAwardEnabled(true);
        fixedActivity.setSubgroup(subgroup);

        Student studentA = new Student();
        studentA.setId(101L);
        studentA.setFullName("Student A");
        studentA.setRegNo("STU_A");
        studentA.setStage(1);

        Student studentB = new Student();
        studentB.setId(102L);
        studentB.setFullName("Student B");
        studentB.setRegNo("STU_B");
        studentB.setStage(1);

        ActivityAssignment assignment = new ActivityAssignment();
        assignment.setId(501L);
        assignment.setAssignmentScope(AssignmentScope.GLOBAL);
        assignment.setActivity(fixedActivity);

        when(userRepository.findByUsername("teacher1")).thenReturn(Optional.of(teacherUser));
        when(activityRepository.findById(1L)).thenReturn(Optional.of(fixedActivity));
        when(activityAssignmentRepository.findById(501L)).thenReturn(Optional.of(assignment));
        when(activityAssignmentRepository.findByActivityId(1L)).thenReturn(List.of(assignment));
        when(assignmentSecurityService.isUserAssignedFaculty(any(), any())).thenReturn(true);
        when(studentRepository.findAllById(anyCollection())).thenReturn(List.of(studentA, studentB));
        when(studentRepository.findById(101L)).thenReturn(Optional.of(studentA));
        when(studentRepository.findById(102L)).thenReturn(Optional.of(studentB));

        AwardXpRequest batchReq = new AwardXpRequest();
        batchReq.setActivityId(1L);
        batchReq.setAssignmentId(501L);
        batchReq.setStudentIds(List.of(101L, 102L));
        Map<Long, Integer> studentXpMap = new HashMap<>();
        studentXpMap.put(101L, 90);
        studentXpMap.put(102L, 75);
        batchReq.setStudentXpMap(studentXpMap);

        ResponseEntity<?> response = studentXpService.awardStudentXpBatch(batchReq, "teacher1");
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // Both must be awarded 50 XP
        ArgumentCaptor<XpTransaction> txCaptor = ArgumentCaptor.forClass(XpTransaction.class);
        verify(xpTransactionRepository, times(2)).saveAndFlush(txCaptor.capture());
        List<XpTransaction> savedTxs = txCaptor.getAllValues();
        assertEquals(50, savedTxs.get(0).getXpPoints());
        assertEquals(50, savedTxs.get(1).getXpPoints());
    }
}
