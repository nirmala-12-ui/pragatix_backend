package jjcet.PragatiX.modules.admin.service;

import jjcet.PragatiX.entity.*;
import jjcet.PragatiX.enums.AcademicYear;
import jjcet.PragatiX.modules.academiccalendar.repository.AcademicMonthRepository;
import jjcet.PragatiX.modules.academiccalendar.repository.AcademicWeekRepository;
import jjcet.PragatiX.modules.activity.repository.ActivityRepository;
import jjcet.PragatiX.modules.activity.repository.ActivityStageRepository;
import jjcet.PragatiX.modules.admin.dto.report.AcademicWeekOptionDto;
import jjcet.PragatiX.modules.admin.dto.report.ActivityPointsReportResponse;
import jjcet.PragatiX.modules.admin.dto.report.StudentReportRowDto;
import jjcet.PragatiX.modules.authentication.repository.UserRepository;
import jjcet.PragatiX.modules.authentication.security.AuthUtils;
import jjcet.PragatiX.modules.student.repository.StudentActivityXpRepository;
import jjcet.PragatiX.modules.student.repository.StudentRepository;
import jjcet.PragatiX.repository.ActivityAssignmentRepository;
import jjcet.PragatiX.repository.DepartmentRepository;
import jjcet.PragatiX.repository.PenaltyRequestRepository;
import jjcet.PragatiX.repository.TeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ActivityReportServiceTest {

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private StudentActivityXpRepository studentActivityXpRepository;

    @Mock
    private AcademicMonthRepository academicMonthRepository;

    @Mock
    private AcademicWeekRepository academicWeekRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private ActivityAssignmentRepository activityAssignmentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthUtils authUtils;

    @Mock
    private PenaltyRequestRepository penaltyRequestRepository;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private ActivityStageRepository activityStageRepository;

    @InjectMocks
    private ActivityReportService reportService;

    private User superAdminUser;
    private Department cseDept;
    private Department eceDept;
    private Section secA;
    private Year year1;
    private Student student1;
    private Student student2;
    private User teacher1;

    @BeforeEach
    void setUp() {
        superAdminUser = new User();
        superAdminUser.setId(1L);
        superAdminUser.setUsername("superadmin");

        cseDept = new Department();
        cseDept.setId(10L);
        cseDept.setName("Computer Science and Engineering");

        eceDept = new Department();
        eceDept.setId(20L);
        eceDept.setName("Electronics and Communication Engineering");

        secA = new Section();
        secA.setId(101L);
        secA.setSectionName("A");

        year1 = new Year();
        year1.setId(1L);
        year1.setYearNo((byte) 1);
        year1.setYearName("1st Year");

        teacher1 = new User();
        teacher1.setId(50L);
        teacher1.setFullName("Prof. Sharugesh");

        student1 = new Student();
        student1.setId(1001L);
        student1.setFullName("Aarav Kumar");
        student1.setRegNo("91001");
        student1.setSprNo("SPR1001");
        student1.setDepartment(cseDept);
        student1.setSection(secA);
        student1.setYearRef(year1);
        student1.setActive(true);

        student2 = new Student();
        student2.setId(1002L);
        student2.setFullName("Bhavna Patel");
        student2.setRegNo("91002");
        student2.setSprNo("SPR1002");
        student2.setDepartment(eceDept);
        student2.setSection(secA);
        student2.setYearRef(year1);
        student2.setActive(true);

        lenient().when(authUtils.getCurrentUser()).thenReturn(superAdminUser);
        lenient().when(authUtils.isSuperAdmin(superAdminUser)).thenReturn(true);
        lenient().when(studentRepository.findAll()).thenReturn(List.of(student1, student2));
    }

    @Test
    void testOneTimeFixedXpReport() {
        Activity act = new Activity();
        act.setId(1L);
        act.setName("Technical Quiz");
        act.setDescription("Technical knowledge assessment");
        act.setAwardFrequency("One Time");
        act.setAwardType("Fixed XP");
        act.setAwardXp(20);
        act.setMaximumAwards(1);
        act.setAssignmentMode("GLOBAL");

        when(activityRepository.findById(1L)).thenReturn(Optional.of(act));

        StudentActivityXp award = new StudentActivityXp();
        award.setStudent(student1);
        award.setActivity(act);
        award.setTeacher(teacher1);
        award.setXpAwarded(20);
        award.setAwardedAt(LocalDateTime.of(2026, 10, 6, 10, 0));
        award.setRemarks("Excellent");

        when(studentActivityXpRepository.findByActivityIdOrderByAwardedAtAsc(1L))
                .thenReturn(List.of(award));

        ActivityPointsReportResponse res = reportService.generateActivityPointsReport(
                "FIRST_YEAR", null, null, null, null, null, 1L,
                null, null, null, null, null, 0, 50);

        assertNotNull(res);
        assertEquals("Technical Quiz", res.getActivityDetails().getActivityName());
        assertEquals("ONE_TIME", res.getActivityDetails().getFrequencyNormalized());
        assertFalse(res.getActivityDetails().isVariableXp());
        assertEquals(20, res.getActivityDetails().getPointsPerCap());
        assertEquals(20, res.getActivityDetails().getTotalPossiblePoints());
        assertEquals("-", res.getActivityDetails().getAwardedByStaff()); // All departments

        // Summary
        assertEquals(2, res.getSummary().getTotalStudents());
        assertEquals(1, res.getSummary().getAwardedCount());
        assertEquals(1, res.getSummary().getNotAwardedCount());
        assertEquals(50.0, res.getSummary().getAwardedPercentage());

        // Students
        assertEquals(2, res.getStudents().size());
        assertEquals("Awarded", res.getStudents().get(0).getStatus());
        assertNull(res.getStudents().get(0).getPointsDisplay()); // No Points column for Fixed XP!
        assertEquals("Not Awarded", res.getStudents().get(1).getStatus());

        // Department Summary should be present when Department = All
        assertNotNull(res.getDepartmentSummary());
        assertEquals(2, res.getDepartmentSummary().size());
    }

    @Test
    void testVariableXpBoundaryConditions() {
        Activity act = new Activity();
        act.setId(2L);
        act.setName("Coding Contest");
        act.setAwardFrequency("One Time");
        act.setAwardType("Variable XP");
        act.setMaxPoints(50);
        act.setMaximumAwards(1);

        when(activityRepository.findById(2L)).thenReturn(Optional.of(act));

        // student1 gets 50 (max) -> Awarded
        StudentActivityXp award1 = new StudentActivityXp();
        award1.setStudent(student1);
        award1.setActivity(act);
        award1.setTeacher(teacher1);
        award1.setXpAwarded(50);
        award1.setAwardedAt(LocalDateTime.now());

        // student2 gets 25 (partial) -> Partial Awarded
        StudentActivityXp award2 = new StudentActivityXp();
        award2.setStudent(student2);
        award2.setActivity(act);
        award2.setTeacher(teacher1);
        award2.setXpAwarded(25);
        award2.setAwardedAt(LocalDateTime.now());

        when(studentActivityXpRepository.findByActivityIdOrderByAwardedAtAsc(2L))
                .thenReturn(List.of(award1, award2));

        ActivityPointsReportResponse res = reportService.generateActivityPointsReport(
                "FIRST_YEAR", null, null, null, null, null, 2L,
                null, null, null, null, null, 0, 50);

        assertTrue(res.getActivityDetails().isVariableXp());
        assertEquals("Awarded", res.getStudents().get(0).getStatus());
        assertEquals("50 / 50", res.getStudents().get(0).getPointsDisplay());

        assertEquals("Partial Awarded", res.getStudents().get(1).getStatus());
        assertEquals("25 / 50", res.getStudents().get(1).getPointsDisplay());

        assertEquals(1, res.getSummary().getFullyAwardedCount());
        assertEquals(1, res.getSummary().getPartiallyAwardedCount());
        assertEquals(0, res.getSummary().getNotAwardedCount());
    }

    @Test
    void testWeeklyDynamicCapLogic() {
        Activity act = new Activity();
        act.setId(3L);
        act.setName("Project Review");
        act.setAwardFrequency("Weekly");
        act.setAwardType("Variable XP");
        act.setMaxPoints(50);
        act.setMaximumAwards(2); // CAP Limit = 2 -> Total Possible = 100

        when(activityRepository.findById(3L)).thenReturn(Optional.of(act));

        // CAP 1 award: 40 points
        StudentActivityXp cap1 = new StudentActivityXp();
        cap1.setStudent(student1);
        cap1.setActivity(act);
        cap1.setTeacher(teacher1);
        cap1.setXpAwarded(40);
        cap1.setAwardedAt(LocalDateTime.of(2026, 10, 5, 9, 0));

        // CAP 2 award: 35 points
        StudentActivityXp cap2 = new StudentActivityXp();
        cap2.setStudent(student1);
        cap2.setActivity(act);
        cap2.setTeacher(teacher1);
        cap2.setXpAwarded(35);
        cap2.setAwardedAt(LocalDateTime.of(2026, 10, 7, 14, 0));

        when(studentActivityXpRepository.findByActivityIdAndDateRange(eq(3L), any(), any()))
                .thenReturn(List.of(cap1, cap2));

        ActivityPointsReportResponse res = reportService.generateActivityPointsReport(
                "FIRST_YEAR", null, null, null, null, null, 3L,
                null, 10, 2026, 0, null, 0, 50);

        assertEquals("WEEKLY", res.getActivityDetails().getFrequencyNormalized());
        assertEquals(2, res.getActivityDetails().getCapLimit());
        assertEquals(100, res.getActivityDetails().getTotalPossiblePoints());
        assertEquals(2, res.getCapColumns().size());
        assertEquals("CAP 1", res.getCapColumns().get(0).getLabel());
        assertEquals("CAP 2", res.getCapColumns().get(1).getLabel());

        // Student 1 has CAP 1 = 40, CAP 2 = 35 -> Total = 75 / 100 -> Partial Awarded
        var s1Row = res.getStudents().get(0);
        assertEquals(75, s1Row.getTotalPoints());
        assertEquals("75 / 100", s1Row.getPointsDisplay());
        assertEquals("Partial Awarded", s1Row.getStatus());
        assertEquals(2, s1Row.getCapAwards().size());
        assertEquals(40, s1Row.getCapAwards().get(0).getPoints());
        assertEquals(35, s1Row.getCapAwards().get(1).getPoints());
    }

    @Test
    void testDailyActivityReport() {
        Activity act = new Activity();
        act.setId(4L);
        act.setName("Class Notes");
        act.setAwardFrequency("Daily");
        act.setAwardType("Fixed XP");
        act.setAwardXp(10);
        act.setMaximumAwards(1);

        when(activityRepository.findById(4L)).thenReturn(Optional.of(act));

        LocalDate queryDate = LocalDate.of(2026, 10, 6);
        StudentActivityXp award = new StudentActivityXp();
        award.setStudent(student1);
        award.setActivity(act);
        award.setTeacher(teacher1);
        award.setXpAwarded(10);
        award.setAwardedAt(queryDate.atTime(11, 0));

        when(studentActivityXpRepository.findByActivityIdAndDateRange(eq(4L), any(), any()))
                .thenReturn(List.of(award));

        ActivityPointsReportResponse res = reportService.generateActivityPointsReport(
                "FIRST_YEAR", null, null, null, null, null, 4L,
                queryDate, null, null, null, null, 0, 50);

        assertEquals("DAILY", res.getActivityDetails().getFrequencyNormalized());
        assertEquals("06 October 2026", res.getActiveTimeFilterLabel());
        assertEquals("Awarded", res.getStudents().get(0).getStatus());
    }

    @Test
    void testParticularClassStaffResolution() {
        Activity act = new Activity();
        act.setId(5L);
        act.setName("Lab Assessment");
        act.setAwardFrequency("One Time");
        act.setAwardType("Fixed XP");
        act.setAwardXp(25);
        act.setMaximumAwards(1);

        when(activityRepository.findById(5L)).thenReturn(Optional.of(act));

        StudentActivityXp award = new StudentActivityXp();
        award.setStudent(student1);
        award.setActivity(act);
        award.setTeacher(teacher1);
        award.setXpAwarded(25);
        award.setAwardedAt(LocalDateTime.now());

        when(studentActivityXpRepository.findByActivityIdOrderByAwardedAtAsc(5L))
                .thenReturn(List.of(award));

        // When Department = 10 (CSE) and Section = 101 (A)
        ActivityPointsReportResponse res = reportService.generateActivityPointsReport(
                "FIRST_YEAR", null, 10L, 101L, null, null, 5L,
                null, null, null, null, null, 0, 50);

        // Awarded by staff in Activity Details should display actual staff name!
        assertEquals("PROF. SHARUGESH", res.getActivityDetails().getAwardedByStaff());
        // Department summary should be empty when a specific department is chosen
        assertTrue(res.getDepartmentSummary().isEmpty());
    }

    @Test
    void testGetWeeksForMonth() {
        List<AcademicWeekOptionDto> weeks = reportService.getWeeksForMonth(10, 2026, "FIRST_YEAR");
        assertNotNull(weeks);
        assertFalse(weeks.isEmpty());
        assertTrue(weeks.size() >= 4);
        assertEquals(1, weeks.get(0).getWeekNumber());
        assertEquals("Week 1", weeks.get(0).getLabel());
    }

    @Test
    void testGroupActivityReportWithCaptainAndViceCaptain() {
        Activity act = new Activity();
        act.setId(6L);
        act.setName("Team Math HOT");
        act.setAwardFrequency("One Time");
        act.setAwardType("Fixed XP");
        act.setAwardXp(100);
        act.setMaximumAwards(1);
        act.setLegacySubgroup("Groups");
        act.setAssignmentMode("GLOBAL");

        when(activityRepository.findById(6L)).thenReturn(Optional.of(act));

        Team team = new Team();
        team.setId(10L);
        team.setName("Team Innovators");
        team.setCaptain(student1);
        team.setViceCaptain(student2);
        team.setMembers(Set.of(student1, student2));

        when(teamRepository.findAll()).thenReturn(List.of(team));

        ActivityPointsReportResponse res = reportService.generateActivityPointsReport(
                "FIRST_YEAR", null, null, null, null, null, 6L,
                null, null, null, null, null, 0, 50);

        assertNotNull(res);
        assertEquals(2, res.getStudents().size());

        // Captain student1 should be first
        StudentReportRowDto capRow = res.getStudents().get(0);
        assertEquals("Team Innovators", capRow.getTeamName());
        assertEquals("Captain", capRow.getTeamRole());
        assertTrue(capRow.isCaptain());
        assertFalse(capRow.isViceCaptain());

        // Vice Captain student2 should be second
        StudentReportRowDto viceRow = res.getStudents().get(1);
        assertEquals("Team Innovators", viceRow.getTeamName());
        assertEquals("Vice Captain", viceRow.getTeamRole());
        assertFalse(viceRow.isCaptain());
        assertTrue(viceRow.isViceCaptain());
    }
}
