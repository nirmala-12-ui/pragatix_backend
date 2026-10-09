package jjcet.PragatiX.modules.student.service;

import jjcet.PragatiX.entity.Activity;
import jjcet.PragatiX.entity.ActivityAssignment;
import jjcet.PragatiX.entity.AssignmentScope;
import jjcet.PragatiX.entity.Student;
import jjcet.PragatiX.entity.User;
import jjcet.PragatiX.modules.student.dto.response.MyActivityStudentsResponse;
import jjcet.PragatiX.modules.authentication.repository.UserRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class StudentXpMapper {

    private final UserRepository userRepository;

    public StudentXpMapper(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public MyActivityStudentsResponse mapToActivityStudentsResponse(Activity activity,
            ActivityAssignment priorityAssignment, List<Student> studentList) {
        return mapToActivityStudentsResponse(activity, priorityAssignment, studentList, java.util.Collections.emptyList());
    }

    public MyActivityStudentsResponse mapToActivityStudentsResponse(Activity activity,
            ActivityAssignment priorityAssignment, List<Student> studentList,
            List<jjcet.PragatiX.entity.StudentActivityXp> activityAwards) {
        List<MyActivityStudentsResponse.StudentDetail> studentDetails = new ArrayList<>();

        // Group awards by student ID
        java.util.Map<Long, List<jjcet.PragatiX.entity.StudentActivityXp>> awardsByStudent = new java.util.HashMap<>();
        if (activityAwards != null) {
            for (jjcet.PragatiX.entity.StudentActivityXp xp : activityAwards) {
                if (xp != null && xp.getStudent() != null && xp.getStudent().getId() != null) {
                    awardsByStudent.computeIfAbsent(xp.getStudent().getId(), k -> new ArrayList<>()).add(xp);
                }
            }
        }

        String awardFrequency = activity.getAwardFrequency();
        if (awardFrequency == null || awardFrequency.trim().isEmpty()) {
            awardFrequency = activity.getFrequency();
        }
        if (awardFrequency == null || awardFrequency.trim().isEmpty()) {
            awardFrequency = "One Time";
        }

        Integer cap = activity.getMaximumAwards();
        if (cap == null || cap <= 0) cap = activity.getCap();
        if (cap == null || cap <= 0) cap = 1;

        java.time.LocalDate now = java.time.LocalDate.now();
        java.time.LocalDateTime windowStart = null;
        if ("Daily".equalsIgnoreCase(awardFrequency)) {
            windowStart = now.atStartOfDay();
        } else if ("Every Period".equalsIgnoreCase(awardFrequency)) {
            windowStart = now.atStartOfDay();
            if (activity.getMaximumAwards() == null || activity.getMaximumAwards() <= 0) {
                cap = 8;
            }
        } else if ("Weekly".equalsIgnoreCase(awardFrequency)) {
            windowStart = now.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)).atStartOfDay();
        } else if ("Monthly".equalsIgnoreCase(awardFrequency)) {
            windowStart = now.withDayOfMonth(1).atStartOfDay();
        }

        java.time.format.DateTimeFormatter timeFmt = java.time.format.DateTimeFormatter.ofPattern("hh:mm a");
        java.time.format.DateTimeFormatter dateFmt = java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

        for (Student s : studentList) {
            String secName = s.getSection() != null ? s.getSection().getSectionName() : "";
            MyActivityStudentsResponse.StudentDetail detail = new MyActivityStudentsResponse.StudentDetail(
                    s.getId(),
                    s.getFullName(),
                    s.getRegNo(),
                    s.getSprNo(),
                    s.getDepartment() != null ? s.getDepartment().getName() : "",
                    secName,
                    s.getYear() != null ? s.getYear() : "",
                    s.getTotalXp(),
                    s.getScore());

            List<jjcet.PragatiX.entity.StudentActivityXp> studentHistory = awardsByStudent.getOrDefault(s.getId(), java.util.Collections.emptyList());

            int periodCount = 0;
            java.time.LocalDateTime latestAwardTime = null;

            for (jjcet.PragatiX.entity.StudentActivityXp axp : studentHistory) {
                if (axp.getAwardedAt() != null) {
                    if (latestAwardTime == null || axp.getAwardedAt().isAfter(latestAwardTime)) {
                        latestAwardTime = axp.getAwardedAt();
                    }
                }
                if (windowStart != null) {
                    if (axp.getAwardedAt() != null && !axp.getAwardedAt().isBefore(windowStart)) {
                        periodCount++;
                    }
                } else {
                    // One Time / Manual
                    periodCount++;
                }
            }

            boolean isAwarded = periodCount > 0;
            boolean isCapReached = periodCount >= cap;

            String formattedLastAward = null;
            if (latestAwardTime != null) {
                if (latestAwardTime.toLocalDate().isEqual(now)) {
                    formattedLastAward = "Today, " + latestAwardTime.format(timeFmt);
                } else if (latestAwardTime.toLocalDate().isEqual(now.minusDays(1))) {
                    formattedLastAward = "Yesterday, " + latestAwardTime.format(timeFmt);
                } else {
                    formattedLastAward = latestAwardTime.format(dateFmt);
                }
            }

            detail.setIsAwarded(isAwarded);
            detail.setIsCapReached(isCapReached);
            detail.setLastAwardedAt(formattedLastAward);
            detail.setAwardsInPeriod(periodCount);
            detail.setPeriodCap(cap);

            studentDetails.add(detail);
        }

        List<String> evidenceList = new ArrayList<>();
        if (activity.getEvidence() != null && !activity.getEvidence().trim().isEmpty()) {
            for (String ev : activity.getEvidence().split(",")) {
                evidenceList.add(ev.trim());
            }
        }

        MyActivityStudentsResponse.ActivityDetail actDetail = new MyActivityStudentsResponse.ActivityDetail(
                activity.getId(),
                activity.getName(),
                activity.getDescription(),
                activity.getOwnerDepartment(),
                evidenceList,
                activity.getFrequency(),
                activity.getType(),
                activity.getXpCategory(),
                activity.getAwardEnabled(),
                activity.getAwardXp(),
                activity.getPenaltyEnabled(),
                activity.getPenaltyXp(),
                activity.getCap());

        String assignedFacultyName = "Any Faculty";
        String assignmentMode = "Global";
        if (priorityAssignment != null) {
            if (priorityAssignment.getAssignmentScope() == AssignmentScope.SPECIFIC_FACULTY) {
                assignedFacultyName = priorityAssignment.getTeacher() != null
                        ? priorityAssignment.getTeacher().getFullName()
                        : "Any Faculty";
                assignmentMode = "Specific Faculty";
            } else if (priorityAssignment.getAssignmentScope() == AssignmentScope.SECTION
                    || priorityAssignment.getAssignmentScope() == AssignmentScope.DEPARTMENT) {
                assignmentMode = "Class Coordinator (Auto Assigned)";
                assignedFacultyName = "Class Coordinator (Auto Assigned)";
                if (priorityAssignment.getDepartment() != null && priorityAssignment.getSection() != null) {
                    List<User> ccs = userRepository.findClassCoordinatorsByDepartmentAndSection(
                            priorityAssignment.getDepartment().getId(),
                            priorityAssignment.getSection().getId());
                    if (!ccs.isEmpty()) {
                        assignedFacultyName = ccs.get(0).getFullName();
                    }
                }
            }
        }

        MyActivityStudentsResponse.AssignmentDetail assignDetail = new MyActivityStudentsResponse.AssignmentDetail(
                priorityAssignment != null ? priorityAssignment.getId() : null,
                (priorityAssignment != null && priorityAssignment.getAssignedBy() != null) ? priorityAssignment.getAssignedBy().getFullName() : "",
                (priorityAssignment != null && priorityAssignment.getAssignedAt() != null) ? priorityAssignment.getAssignedAt().toString() : "",
                assignedFacultyName,
                assignmentMode);

        int xpLimit = 0;
        try {
            if (activity != null && activity.getXp() != null) {
                xpLimit = Integer.parseInt(activity.getXp());
            }
        } catch (Exception ignored) {
        }

        return new MyActivityStudentsResponse(actDetail, studentDetails, xpLimit, assignDetail);
    }
}
