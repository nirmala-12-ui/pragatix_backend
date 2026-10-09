package jjcet.PragatiX.modules.student.service;

import jjcet.PragatiX.entity.Activity;
import jjcet.PragatiX.entity.Student;
import jjcet.PragatiX.entity.StudentActivityXp;
import jjcet.PragatiX.entity.User;
import jjcet.PragatiX.modules.academiccalendar.service.AcademicCalendarResolver;
import jjcet.PragatiX.modules.student.repository.StudentActivityXpRepository;
import jjcet.PragatiX.repository.ActivityAssignmentRepository;
import jjcet.PragatiX.repository.TeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class StudentXpValidatorTest {

    @Mock
    private StudentActivityXpRepository studentActivityXpRepository;
    @Mock
    private AcademicCalendarResolver academicCalendarResolver;
    @Mock
    private TeamRepository teamRepository;
    @Mock
    private ActivityAssignmentRepository activityAssignmentRepository;

    private StudentXpValidator validator;

    @BeforeEach
    void setUp() {
        validator = new StudentXpValidator(
                studentActivityXpRepository,
                academicCalendarResolver,
                teamRepository,
                activityAssignmentRepository
        );
    }

    @Test
    void testDailyActivity_Cap1_FirstTime_Allowed() {
        Student student = new Student();
        student.setId(100L);
        student.setFullName("Test Student");
        student.setStage(1);

        Activity activity = new Activity();
        activity.setId(1L);
        activity.setAwardFrequency("Daily");
        activity.setMaximumAwards(1);
        activity.setRepeatAllowed(true);

        when(studentActivityXpRepository.findByStudentIdAndActivityIdAndStage(100L, 1L, 1))
                .thenReturn(new ArrayList<>());

        String error = validator.checkAwardLimit(student, activity);
        assertNull(error, "First time award for daily activity should be allowed");
    }

    @Test
    void testDailyActivity_Cap1_SecondTimeToday_Blocked() {
        Student student = new Student();
        student.setId(100L);
        student.setFullName("Test Student");
        student.setStage(1);

        Activity activity = new Activity();
        activity.setId(1L);
        activity.setAwardFrequency("Daily");
        activity.setMaximumAwards(1);
        activity.setRepeatAllowed(true);

        List<StudentActivityXp> history = new ArrayList<>();
        StudentActivityXp xpToday = new StudentActivityXp();
        xpToday.setAwardedAt(LocalDateTime.now());
        history.add(xpToday);

        when(studentActivityXpRepository.findByStudentIdAndActivityIdAndStage(100L, 1L, 1))
                .thenReturn(history);

        String error = validator.checkAwardLimit(student, activity);
        assertNotNull(error, "Second award on the same day should be blocked");
        assertTrue(error.contains("already reached the maximum allowed XP awards (1) for today"));
    }
}
