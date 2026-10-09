package jjcet.PragatiX.modules.admin.service;

import jjcet.PragatiX.entity.*;
import jjcet.PragatiX.enums.AcademicYear;
import jjcet.PragatiX.enums.DepartmentType;
import jjcet.PragatiX.modules.academiccalendar.repository.AcademicMonthRepository;
import jjcet.PragatiX.modules.academiccalendar.repository.AcademicWeekRepository;
import jjcet.PragatiX.modules.activity.repository.ActivityRepository;
import jjcet.PragatiX.modules.activity.repository.ActivityStageRepository;
import jjcet.PragatiX.modules.admin.dto.report.*;
import jjcet.PragatiX.modules.authentication.repository.UserRepository;
import jjcet.PragatiX.modules.authentication.security.AuthUtils;
import jjcet.PragatiX.modules.student.repository.StudentActivityXpRepository;
import jjcet.PragatiX.modules.student.repository.StudentRepository;
import jjcet.PragatiX.repository.ActivityAssignmentRepository;
import jjcet.PragatiX.repository.DepartmentRepository;
import jjcet.PragatiX.repository.PenaltyRequestRepository;
import jjcet.PragatiX.repository.TeamRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ActivityReportService {

    private static final Logger log = LoggerFactory.getLogger(ActivityReportService.class);
    private static final DateTimeFormatter DISPLAY_DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
    private static final DateTimeFormatter RANGE_DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final ActivityRepository activityRepository;
    private final StudentRepository studentRepository;
    private final StudentActivityXpRepository studentActivityXpRepository;
    private final AcademicMonthRepository academicMonthRepository;
    private final AcademicWeekRepository academicWeekRepository;
    private final DepartmentRepository departmentRepository;
    private final ActivityAssignmentRepository activityAssignmentRepository;
    private final UserRepository userRepository;
    private final AuthUtils authUtils;
    private final PenaltyRequestRepository penaltyRequestRepository;
    private final TeamRepository teamRepository;
    private final ActivityStageRepository activityStageRepository;

    public ActivityReportService(
            ActivityRepository activityRepository,
            StudentRepository studentRepository,
            StudentActivityXpRepository studentActivityXpRepository,
            AcademicMonthRepository academicMonthRepository,
            AcademicWeekRepository academicWeekRepository,
            DepartmentRepository departmentRepository,
            ActivityAssignmentRepository activityAssignmentRepository,
            UserRepository userRepository,
            AuthUtils authUtils,
            PenaltyRequestRepository penaltyRequestRepository,
            TeamRepository teamRepository,
            ActivityStageRepository activityStageRepository) {
        this.activityRepository = activityRepository;
        this.studentRepository = studentRepository;
        this.studentActivityXpRepository = studentActivityXpRepository;
        this.academicMonthRepository = academicMonthRepository;
        this.academicWeekRepository = academicWeekRepository;
        this.departmentRepository = departmentRepository;
        this.activityAssignmentRepository = activityAssignmentRepository;
        this.userRepository = userRepository;
        this.authUtils = authUtils;
        this.penaltyRequestRepository = penaltyRequestRepository;
        this.teamRepository = teamRepository;
        this.activityStageRepository = activityStageRepository;
    }

    public List<AcademicWeekOptionDto> getWeeksForMonth(Integer month, Integer year, String academicYearStr) {
        if (month == null || month < 1 || month > 12) {
            month = LocalDate.now().getMonthValue();
        }
        if (year == null || year < 2000) {
            year = LocalDate.now().getYear();
        }

        AcademicYear targetYearEnum = AcademicYear.fromString(academicYearStr);
        if (targetYearEnum == null) {
            User currentUser = authUtils.getCurrentUser();
            if (currentUser != null && !authUtils.isSuperAdmin(currentUser)) {
                targetYearEnum = currentUser.getAcademicYear();
            }
        }

        List<AcademicWeekOptionDto> weekOptions = new ArrayList<>();
        if (targetYearEnum != null) {
            Optional<AcademicMonth> academicMonthOpt = academicMonthRepository
                    .findByMonthAndYearAndAcademicYearEnum(month, year, targetYearEnum);
            if (academicMonthOpt.isPresent()) {
                List<AcademicWeek> weeksInDb = academicWeekRepository.findByAcademicMonthId(academicMonthOpt.get().getId());
                if (weeksInDb != null && !weeksInDb.isEmpty()) {
                    weeksInDb.sort(Comparator.comparing(AcademicWeek::getWeekNumber));
                    for (AcademicWeek w : weeksInDb) {
                        String range = w.getStartDate().format(RANGE_DATE_FMT) + " – " + w.getEndDate().format(RANGE_DATE_FMT);
                        weekOptions.add(new AcademicWeekOptionDto(w.getWeekNumber(), "Week " + w.getWeekNumber(),
                                w.getStartDate(), w.getEndDate(), range));
                    }
                    return weekOptions;
                }
            }
        }

        // Standard calendar week partitioning fallback
        LocalDate firstDay = LocalDate.of(year, month, 1);
        int totalDays = firstDay.lengthOfMonth();
        int curStart = 1;
        int weekNum = 1;

        while (curStart <= totalDays) {
            int curEnd = Math.min(curStart + 6, totalDays);
            LocalDate start = LocalDate.of(year, month, curStart);
            LocalDate end = LocalDate.of(year, month, curEnd);
            String range = start.format(RANGE_DATE_FMT) + " – " + end.format(RANGE_DATE_FMT);
            weekOptions.add(new AcademicWeekOptionDto(weekNum, "Week " + weekNum, start, end, range));
            curStart += 7;
            weekNum++;
        }

        return weekOptions;
    }

    public ActivityPointsReportResponse generateActivityPointsReport(
            String academicYearStr,
            Long semesterId,
            Long departmentId,
            Long sectionId,
            Long stageId,
            Long subgroupId,
            Long activityId,
            LocalDate date,
            Integer month,
            Integer year,
            Integer weekNumber,
            String search,
            int page,
            int size) {

        if (activityId == null) {
            throw new IllegalArgumentException("activityId is required to generate report");
        }

        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new IllegalArgumentException("Activity not found with id: " + activityId));

        // Enforce user authorization scope
        User currentUser = authUtils.getCurrentUser();
        AcademicYear effectiveAcademicYear = resolveEffectiveAcademicYear(currentUser, academicYearStr);

        // Normalize Activity Frequency & Mode
        String rawFreq = activity.getAwardFrequency();
        if (rawFreq == null || rawFreq.trim().isEmpty()) {
            rawFreq = activity.getFrequency();
        }
        if (rawFreq == null || rawFreq.trim().isEmpty()) {
            rawFreq = "One Time";
        }

        String freqNorm;
        String freqDisplay;
        String cleanFreq = rawFreq.trim().toUpperCase();
        int configuredCap = 1;

        if (cleanFreq.contains("DAILY")) {
            freqNorm = "DAILY";
            configuredCap = activity.getMaximumAwards() != null && activity.getMaximumAwards() > 0
                    ? activity.getMaximumAwards()
                    : 1;
            freqDisplay = "Daily (" + configuredCap + " time" + (configuredCap > 1 ? "s" : "") + " per day)";
        } else if (cleanFreq.contains("WEEK")) {
            freqNorm = "WEEKLY";
            configuredCap = activity.getMaximumAwards() != null && activity.getMaximumAwards() > 0
                    ? activity.getMaximumAwards()
                    : 1;
            freqDisplay = "Weekly (" + configuredCap + " time" + (configuredCap > 1 ? "s" : "") + " per week)";
        } else if (cleanFreq.contains("MONTH")) {
            freqNorm = "MONTHLY";
            configuredCap = activity.getMaximumAwards() != null && activity.getMaximumAwards() > 0
                    ? activity.getMaximumAwards()
                    : 1;
            freqDisplay = "Monthly (" + configuredCap + " time" + (configuredCap > 1 ? "s" : "") + " per month)";
        } else {
            // One Time frequency: by definition, only 1 award / 1 cap is possible
            freqNorm = "ONE_TIME";
            freqDisplay = "One Time";
            configuredCap = 1;
        }

        // Activity Mode (Award only, Penalty only, or Both)
        boolean awardEnabled = Boolean.TRUE.equals(activity.getAwardEnabled());
        boolean penaltyEnabled = Boolean.TRUE.equals(activity.getPenaltyEnabled());
        String activityMode;
        if (awardEnabled && penaltyEnabled) {
            activityMode = "BOTH";
        } else if (penaltyEnabled && !awardEnabled) {
            activityMode = "PENALTY";
        } else {
            activityMode = "AWARD";
        }

        // Fixed XP vs Variable XP
        boolean isVariableXp = activity.isVariableXp();
        int pointsPerCap;
        if (isVariableXp) {
            pointsPerCap = activity.getAwardXp() != null && activity.getAwardXp() > 0 ? activity.getAwardXp()
                    : (activity.getMaxPoints() > 0 ? activity.getMaxPoints() : 0);
        } else if ("PENALTY".equals(activityMode)) {
            pointsPerCap = activity.getPenaltyXp() != null && activity.getPenaltyXp() > 0 ? activity.getPenaltyXp()
                    : (activity.getMaxPoints() > 0 ? activity.getMaxPoints() : 10);
        } else {
            pointsPerCap = activity.getAwardXp() != null && activity.getAwardXp() > 0 ? activity.getAwardXp()
                    : activity.getMaxPoints();
        }
        int totalPossiblePoints = configuredCap * pointsPerCap;

        // Resolve Date Window based on Frequency
        LocalDateTime startDate = null;
        LocalDateTime endDate = null;
        String timeFilterLabel = "";

        if ("DAILY".equals(freqNorm)) {
            LocalDate targetDate = (date != null) ? date : LocalDate.now();
            startDate = targetDate.atStartOfDay();
            endDate = targetDate.atTime(23, 59, 59);
            timeFilterLabel = targetDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy"));
        } else if ("WEEKLY".equals(freqNorm)) {
            int targetMonth = (month != null && month >= 1 && month <= 12) ? month : LocalDate.now().getMonthValue();
            int targetYear = (year != null && year >= 2000) ? year : LocalDate.now().getYear();
            List<AcademicWeekOptionDto> availableWeeks = getWeeksForMonth(targetMonth, targetYear,
                    effectiveAcademicYear != null ? effectiveAcademicYear.name() : null);

            if (weekNumber != null && weekNumber > 0) {
                AcademicWeekOptionDto targetWeek = availableWeeks.stream()
                        .filter(w -> w.getWeekNumber() == weekNumber)
                        .findFirst()
                        .orElse(availableWeeks.isEmpty() ? null : availableWeeks.get(0));

                if (targetWeek != null) {
                    startDate = targetWeek.getStartDate().atStartOfDay();
                    endDate = targetWeek.getEndDate().atTime(23, 59, 59);
                    timeFilterLabel = Month.of(targetMonth).name() + " " + targetYear + " (" + targetWeek.getLabel() + ": " + targetWeek.getDateRangeLabel() + ")";
                }
            } else {
                // All Weeks
                LocalDate mStart = LocalDate.of(targetYear, targetMonth, 1);
                LocalDate mEnd = mStart.withDayOfMonth(mStart.lengthOfMonth());
                startDate = mStart.atStartOfDay();
                endDate = mEnd.atTime(23, 59, 59);
                timeFilterLabel = Month.of(targetMonth).name() + " " + targetYear + " (All Weeks: " + mStart.format(RANGE_DATE_FMT) + " – " + mEnd.format(RANGE_DATE_FMT) + ")";
            }
        } else if ("MONTHLY".equals(freqNorm)) {
            int targetMonth = (month != null && month >= 1 && month <= 12) ? month : LocalDate.now().getMonthValue();
            int targetYear = (year != null && year >= 2000) ? year : LocalDate.now().getYear();
            LocalDate mStart = LocalDate.of(targetYear, targetMonth, 1);
            LocalDate mEnd = mStart.withDayOfMonth(mStart.lengthOfMonth());
            startDate = mStart.atStartOfDay();
            endDate = mEnd.atTime(23, 59, 59);
            timeFilterLabel = Month.of(targetMonth).name() + " " + targetYear;
        } else {
            timeFilterLabel = "One Time (All History)";
        }

        // Check if group activity: only if subgroup category or name is group-based and not individual
        boolean isGroupActivity = (activity.getSubgroup() != null && "group".equalsIgnoreCase(activity.getSubgroup().getCategory()))
                || (activity.getSubgroup() != null && activity.getSubgroup().getName() != null
                    && activity.getSubgroup().getName().toLowerCase().contains("group")
                    && !activity.getSubgroup().getName().toLowerCase().contains("individual"))
                || "GROUP".equalsIgnoreCase(activity.getModeType())
                || "GROUP".equalsIgnoreCase(activity.getType())
                || (activity.getLegacySubgroup() != null
                    && activity.getLegacySubgroup().toLowerCase().contains("group")
                    && !activity.getLegacySubgroup().toLowerCase().contains("individual"));

        // Preload teams to map students to their groups, captain, and vice captain
        Map<Long, Team> studentToTeamMap = new HashMap<>();
        Map<Long, String> studentToRoleMap = new HashMap<>();
        try {
            if (teamRepository != null) {
                List<Team> allTeams = teamRepository.findAll();
                for (Team t : allTeams) {
                    if (t.isDeleted()) continue;
                    if (t.getCaptain() != null && t.getCaptain().getId() != null) {
                        studentToTeamMap.put(t.getCaptain().getId(), t);
                        studentToRoleMap.put(t.getCaptain().getId(), "Captain");
                    }
                    if (t.getViceCaptain() != null && t.getViceCaptain().getId() != null) {
                        studentToTeamMap.put(t.getViceCaptain().getId(), t);
                        studentToRoleMap.put(t.getViceCaptain().getId(), "Vice Captain");
                    }
                    if (t.getMembers() != null) {
                        for (Student m : t.getMembers()) {
                            if (m != null && m.getId() != null) {
                                studentToTeamMap.putIfAbsent(m.getId(), t);
                                studentToRoleMap.putIfAbsent(m.getId(), "Member");
                            }
                        }
                    }
                }
            }
            if (studentRepository != null) {
                List<Student> allSts = studentRepository.findAll();
                for (Student s : allSts) {
                    if (s.getTeam() != null && s.getTeam().getId() != null) {
                        studentToTeamMap.putIfAbsent(s.getId(), s.getTeam());
                        if (!studentToRoleMap.containsKey(s.getId())) {
                            Team t = s.getTeam();
                            boolean isCap = (t.getCaptain() != null && s.getId().equals(t.getCaptain().getId())) || s.isCaptain();
                            boolean isVice = (t.getViceCaptain() != null && s.getId().equals(t.getViceCaptain().getId()));
                            if (isCap) studentToRoleMap.put(s.getId(), "Captain");
                            else if (isVice) studentToRoleMap.put(s.getId(), "Vice Captain");
                            else studentToRoleMap.put(s.getId(), "Member");
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to preload teams: {}", e.getMessage());
        }

        // Fetch eligible students based on filter parameters
        List<Student> allEligibleStudents = fetchEligibleStudents(effectiveAcademicYear, semesterId, departmentId, sectionId, stageId);

        // For group activities, only include students who belong to teams
        if (isGroupActivity) {
            allEligibleStudents = allEligibleStudents.stream()
                    .filter(s -> studentToTeamMap.containsKey(s.getId()) || s.getTeam() != null)
                    .collect(Collectors.toList());
        }

        // Fetch activity awards in window
        List<StudentActivityXp> awardsInWindow;
        if (startDate != null && endDate != null) {
            awardsInWindow = studentActivityXpRepository.findByActivityIdAndDateRange(activityId, startDate, endDate);
        } else {
            awardsInWindow = studentActivityXpRepository.findByActivityIdOrderByAwardedAtAsc(activityId);
        }

        Map<Long, List<StudentActivityXp>> studentAwardsMap = new HashMap<>();
        for (StudentActivityXp axp : awardsInWindow) {
            if (axp.getStudent() != null && axp.getStudent().getId() != null) {
                studentAwardsMap.computeIfAbsent(axp.getStudent().getId(), k -> new ArrayList<>()).add(axp);
            }
        }

        boolean isPenaltyActivity = Boolean.TRUE.equals(activity.getPenaltyEnabled());
        Map<Long, PenaltyRequest> penaltyByStudentMap = new HashMap<>();
        if (isPenaltyActivity) {
            try {
                if (penaltyRequestRepository != null) {
                    List<PenaltyRequest> penalties = penaltyRequestRepository.findByActivityIdAndNotRejected(activityId);
                    // Sort by date ascending so latest penalty within window takes precedence
                    penalties.sort(Comparator.comparing(
                            p -> (p.getApprovedAt() != null ? p.getApprovedAt() : p.getCreatedAt()),
                            Comparator.nullsLast(Comparator.naturalOrder())));
                    for (PenaltyRequest pr : penalties) {
                        if (pr.getStudent() != null && pr.getStudent().getId() != null) {
                            if (startDate != null && endDate != null) {
                                LocalDateTime pDate = pr.getApprovedAt() != null ? pr.getApprovedAt() : pr.getCreatedAt();
                                boolean inWindow = (pDate != null && !pDate.isBefore(startDate) && !pDate.isAfter(endDate))
                                        || (pr.getCreatedAt() != null && !pr.getCreatedAt().isBefore(startDate) && !pr.getCreatedAt().isAfter(endDate));
                                if (!inWindow) {
                                    continue;
                                }
                            }
                            penaltyByStudentMap.put(pr.getStudent().getId(), pr);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Could not query penalties for activity {}: {}", activityId, e.getMessage());
            }
        }

        // Filter eligible students strictly by activity assignment:
        // If an activity is assigned to specific classes (departments/sections/year), only include students of those assigned classes.
        List<ActivityAssignment> activityAssignments = activityAssignmentRepository != null
                ? activityAssignmentRepository.findByActivityId(activityId)
                : Collections.emptyList();

        Integer targetStageOrderForAssign = null;
        if (stageId != null && stageId > 0 && activityStageRepository != null) {
            try {
                Optional<ActivityStage> stOpt = activityStageRepository.findById(stageId);
                targetStageOrderForAssign = stOpt.map(ActivityStage::getDisplayOrder).orElse(stageId.intValue());
            } catch (Exception e) {
                targetStageOrderForAssign = stageId.intValue();
            }
        }
        final Integer finalStageOrderForAssign = targetStageOrderForAssign;

        boolean isUniversallyGlobal = Boolean.TRUE.equals(activity.getAttendanceEngineEnabled())
                || ("GLOBAL".equalsIgnoreCase(activity.getAssignmentMode()) && activityAssignments.isEmpty())
                || activityAssignments.stream().anyMatch(a -> a.getAssignmentScope() == AssignmentScope.GLOBAL
                        && a.getDepartment() == null
                        && a.getSection() == null
                        && (a.getYear() == null || a.getYear().trim().isEmpty()));

        if (!isUniversallyGlobal) {
            if (activityAssignments.isEmpty()) {
                // Not global and has no assignments -> not assigned to any class
                allEligibleStudents = allEligibleStudents.stream()
                        .filter(s -> studentAwardsMap.containsKey(s.getId()) || penaltyByStudentMap.containsKey(s.getId()))
                        .collect(Collectors.toList());
            } else {
                // Only include students whose class matches at least one assignment, or who have activity records
                allEligibleStudents = allEligibleStudents.stream()
                        .filter(s -> studentAwardsMap.containsKey(s.getId())
                                || penaltyByStudentMap.containsKey(s.getId())
                                || activityAssignments.stream().anyMatch(a -> isStudentAssignedToActivity(
                                        s, effectiveAcademicYear, finalStageOrderForAssign, stageId, a)))
                        .collect(Collectors.toList());
            }
        }

        // Generate dynamic CAP column definitions
        List<CapColumnDto> capColumns = new ArrayList<>();
        for (int k = 1; k <= configuredCap; k++) {
            capColumns.add(new CapColumnDto(k, "CAP " + k));
        }

        // Awarded by staff logic for Activity Details
        String awardedByStaffInDetails = "-";
        boolean isParticularClass = departmentId != null && departmentId > 0 && sectionId != null && sectionId > 0;
        if (isParticularClass) {
            awardedByStaffInDetails = resolveStaffForParticularClass(activityId, departmentId, sectionId, awardsInWindow);
        }

        // Activity Detail DTO
        ActivityReportDetailDto actDetailDto = new ActivityReportDetailDto();
        actDetailDto.setId(activity.getId());
        actDetailDto.setActivityName(activity.getActivityName() != null ? activity.getActivityName() : activity.getName());
        actDetailDto.setDescription(activity.getActivityDescription() != null ? activity.getActivityDescription() : activity.getDescription());
        actDetailDto.setFrequency(freqDisplay);
        actDetailDto.setFrequencyNormalized(freqNorm);
        actDetailDto.setAwardType("PENALTY".equals(activityMode) ? "Penalty XP" : (isVariableXp ? "Variable XP" : "Fixed XP"));
        actDetailDto.setVariableXp(isVariableXp);
        actDetailDto.setPointsPerCap(pointsPerCap);
        actDetailDto.setCapLimit(configuredCap);
        actDetailDto.setTotalPossiblePoints(totalPossiblePoints);
        actDetailDto.setAwardedByStaff(awardedByStaffInDetails);
        actDetailDto.setStageName(activity.getStage() != null ? activity.getStage().getStageName() : "");
        actDetailDto.setSubgroupName(activity.getSubgroup() != null ? activity.getSubgroup().getName() : "");
        actDetailDto.setAwardEnabled(awardEnabled);
        actDetailDto.setPenaltyEnabled(penaltyEnabled);
        actDetailDto.setAwardXp(activity.getAwardXp() != null ? activity.getAwardXp() : 0);
        actDetailDto.setPenaltyXp(activity.getPenaltyXp() != null ? activity.getPenaltyXp() : 0);
        actDetailDto.setActivityMode(activityMode);
        actDetailDto.setIsGroupActivity(isGroupActivity);

        // Build Student Rows and Aggregate Summary
        List<StudentReportRowDto> allStudentRows = new ArrayList<>();
        int countFullyAwarded = 0;
        int countPartiallyAwarded = 0;
        int countNotAwarded = 0;
        int countPenalized = 0;
        int[] capAwardCounts = new int[configuredCap + 1];

        // Department-wise stats accumulator (preload all active main departments including Civil)
        Map<Long, DeptStatAccumulator> deptStatsMap = new LinkedHashMap<>();
        if (departmentId == null || departmentId == 0) {
            try {
                List<Department> mainDepts = departmentRepository.findByDepartmentTypeAndDeletedFalse(DepartmentType.MAIN);
                if (mainDepts != null) {
                    mainDepts.stream()
                        .filter(d -> d.getName() != null && !d.getName().trim().toUpperCase().startsWith("TEST"))
                        .sorted(Comparator.comparing(Department::getName, String.CASE_INSENSITIVE_ORDER))
                        .forEach(d -> deptStatsMap.put(d.getId(), new DeptStatAccumulator(d.getId(), d.getName())));
                }
            } catch (Exception e) {
                log.warn("Failed to preload main departments: {}", e.getMessage());
            }
        }

        for (Student s : allEligibleStudents) {
            List<StudentActivityXp> studentAwards = studentAwardsMap.getOrDefault(s.getId(), Collections.emptyList());
            // Chronologically ordered awards
            studentAwards.sort(Comparator.comparing(StudentActivityXp::getAwardedAt, Comparator.nullsLast(Comparator.naturalOrder())));

            List<StudentCapAwardDto> capAwards = new ArrayList<>();
            int studentTotalPoints = 0;
            int awardedCapCount = 0;
            String latestAwardedAt = "-";
            String latestAwardedBy = "-";
            String latestRemarks = "-";

            for (int k = 1; k <= configuredCap; k++) {
                int awardIndex = k - 1;
                if (awardIndex < studentAwards.size()) {
                    StudentActivityXp axp = studentAwards.get(awardIndex);
                    int pts = axp.getXpAwarded();
                    boolean capPenalized = pts < 0;
                    String capStatus = capPenalized ? "Penalized" : (pts > 0 ? "Awarded" : "Not Awarded");

                    String formattedAt = axp.getAwardedAt() != null ? axp.getAwardedAt().format(DISPLAY_DATE_FMT) : "-";
                    String byStaff = axp.getTeacher() != null ? axp.getTeacher().getFullName() : "-";
                    String rem = axp.getRemarks() != null ? axp.getRemarks() : "";

                    if (!capPenalized) {
                        studentTotalPoints += pts;
                        awardedCapCount++;
                        capAwardCounts[k]++;
                    }

                    latestAwardedAt = formattedAt;
                    latestAwardedBy = byStaff;
                    latestRemarks = rem;

                    capAwards.add(new StudentCapAwardDto(k, pts, formattedAt, byStaff, rem, capStatus, capPenalized));
                } else {
                    capAwards.add(new StudentCapAwardDto(k, null, "-", "-", "", "Not Awarded", false));
                }
            }

            // Cap the student points to totalPossiblePoints
            if (totalPossiblePoints > 0 && studentTotalPoints > totalPossiblePoints) {
                studentTotalPoints = totalPossiblePoints;
            }

            boolean isFullyAwarded = false;
            boolean isPartiallyAwarded = false;

            // Status determination
            String status;
            if ("PENALTY".equals(activityMode)) {
                status = "Not Awarded";
            } else if (isVariableXp) {
                if (studentTotalPoints >= totalPossiblePoints && totalPossiblePoints > 0) {
                    status = "Awarded";
                    isFullyAwarded = true;
                    countFullyAwarded++;
                } else if (studentTotalPoints > 0) {
                    status = "Partial Awarded";
                    isPartiallyAwarded = true;
                    countPartiallyAwarded++;
                } else {
                    status = "Not Awarded";
                    countNotAwarded++;
                }
            } else {
                if (awardedCapCount >= configuredCap && configuredCap > 0) {
                    status = "Awarded";
                    isFullyAwarded = true;
                    countFullyAwarded++;
                } else if (awardedCapCount > 0) {
                    status = "Partial Awarded";
                    isPartiallyAwarded = true;
                    countPartiallyAwarded++;
                } else {
                    status = "Not Awarded";
                    countNotAwarded++;
                }
            }

            // Penalty determination (ONLY if this activity has penaltyEnabled = true)
            boolean isPenalized = false;
            int penaltyXp = 0;
            String penaltyReason = "";

            String penaltyRequestedBy = null;
            String penaltyApprovedBy = null;
            String penaltyRequestedAt = null;
            String penaltyApprovedAt = null;
            String penaltyStatusVal = null;

            if (isPenaltyActivity) {
                if (penaltyByStudentMap.containsKey(s.getId())) {
                    PenaltyRequest pr = penaltyByStudentMap.get(s.getId());
                    isPenalized = true;
                    penaltyXp = pr.getPenaltyXP();
                    penaltyReason = pr.getReason() != null && !pr.getReason().trim().isEmpty() ? pr.getReason() : "Penalty: " + pr.getStatus();

                    String reqBy = pr.getTeacher() != null && pr.getTeacher().getFullName() != null
                            ? pr.getTeacher().getFullName()
                            : (pr.getTeacherName() != null ? pr.getTeacherName() : "-");

                    String apprBy = pr.getApprovedBy() != null && !pr.getApprovedBy().trim().isEmpty()
                            ? pr.getApprovedBy()
                            : (pr.getCc() != null && pr.getCc().getFullName() != null
                                ? pr.getCc().getFullName()
                                : (pr.getCcName() != null ? pr.getCcName() : "-"));

                    String reqAt = pr.getCreatedAt() != null ? pr.getCreatedAt().format(DISPLAY_DATE_FMT) : "-";
                    String apprAt = pr.getApprovedAt() != null ? pr.getApprovedAt().format(DISPLAY_DATE_FMT) : "-";

                    penaltyRequestedBy = reqBy;
                    penaltyApprovedBy = apprBy;
                    penaltyRequestedAt = reqAt;
                    penaltyApprovedAt = apprAt;
                    penaltyStatusVal = pr.getStatus();

                    if ("BOTH".equals(activityMode)) {
                        if ("PENDING".equalsIgnoreCase(penaltyStatusVal)) {
                            penaltyStatusVal = "AUTO_APPROVED";
                        }
                        latestAwardedBy = reqBy;
                        latestAwardedAt = !"-".equals(apprAt) ? apprAt : reqAt;
                        if (penaltyReason != null && penaltyReason.contains("PENDING")) {
                            penaltyReason = "Penalty applied";
                        }
                    } else if ("APPROVED".equalsIgnoreCase(pr.getStatus()) || "AUTO_APPROVED".equalsIgnoreCase(pr.getStatus())) {
                        if (!"-".equals(apprBy) && !apprBy.equalsIgnoreCase(reqBy)) {
                            latestAwardedBy = "Req: " + reqBy + " (Appr: " + apprBy + ")";
                        } else {
                            latestAwardedBy = reqBy;
                        }
                        latestAwardedAt = !"-".equals(apprAt) ? apprAt : reqAt;
                    } else {
                        latestAwardedBy = "Req: " + reqBy + " (Pending CC)";
                        latestAwardedAt = reqAt;
                    }
                }

                for (StudentActivityXp axp : studentAwards) {
                    if (axp.getXpAwarded() < 0) {
                        isPenalized = true;
                        penaltyXp = Math.abs(axp.getXpAwarded());
                        if (axp.getRemarks() != null && !axp.getRemarks().trim().isEmpty()) {
                            penaltyReason = axp.getRemarks();
                        }
                        if (latestAwardedAt == null || "-".equals(latestAwardedAt)) {
                            latestAwardedAt = axp.getAwardedAt() != null ? axp.getAwardedAt().format(DISPLAY_DATE_FMT) : "-";
                        }
                        if (penaltyRequestedBy == null && axp.getTeacher() != null) {
                            penaltyRequestedBy = axp.getTeacher().getFullName();
                            latestAwardedBy = penaltyRequestedBy;
                        }
                    }
                }

                if (isPenalized) {
                    countPenalized++;

                    // Ensure the penalty is reflected in capAwards if not already marked
                    boolean capAlreadyPenalized = capAwards.stream().anyMatch(StudentCapAwardDto::isPenalized);
                    if (!capAlreadyPenalized && !capAwards.isEmpty()) {
                        StudentCapAwardDto targetCap = capAwards.stream()
                                .filter(c -> "Not Awarded".equals(c.getStatus()))
                                .findFirst()
                                .orElse(capAwards.get(0));
                        targetCap.setPoints(-penaltyXp);
                        targetCap.setStatus("Penalized");
                        targetCap.setPenalized(true);
                        targetCap.setAwardedAt(latestAwardedAt != null && !"-".equals(latestAwardedAt) ? latestAwardedAt : (penaltyApprovedAt != null && !"-".equals(penaltyApprovedAt) ? penaltyApprovedAt : penaltyRequestedAt));
                        targetCap.setAwardedBy(latestAwardedBy != null && !"-".equals(latestAwardedBy) ? latestAwardedBy : penaltyRequestedBy);
                        targetCap.setRemarks(penaltyReason != null && !penaltyReason.trim().isEmpty() ? penaltyReason : latestRemarks);
                    }

                    if ("PENALTY".equals(activityMode)) {
                        status = "Penalized";
                        isFullyAwarded = false;
                        isPartiallyAwarded = false;
                    } else if ("BOTH".equals(activityMode)) {
                        if (isFullyAwarded) {
                            status = "Awarded (Penalized)";
                        } else if (isPartiallyAwarded) {
                            status = "Partial (Penalized)";
                        } else {
                            status = "Penalized";
                        }
                    } else {
                        status = "Penalized";
                    }
                } else if ("PENALTY".equals(activityMode)) {
                    countNotAwarded++;
                }
            }

            int awardPts = studentTotalPoints;
            int penPts = isPenalized ? penaltyXp : 0;
            int netPts = awardPts - penPts;

            StudentReportRowDto row = new StudentReportRowDto();
            row.setId(s.getId());
            row.setSprNo(s.getSprNo() != null ? s.getSprNo() : "");
            row.setRegNo(s.getRegNo() != null ? s.getRegNo() : "");
            row.setStudentName(s.getFullName() != null ? s.getFullName() : "");
            row.setDepartment(s.getDepartment() != null ? s.getDepartment().getName() : "");
            row.setSection(s.getSection() != null ? s.getSection().getSectionName() : "");
            row.setStage(activity.getStage() != null ? activity.getStage().getStageName() : "");
            row.setSubgroup(activity.getSubgroup() != null ? activity.getSubgroup().getName() : "");
            row.setCapAwards(capAwards);
            row.setAwardPoints(awardPts);
            row.setNetPoints(netPts);
            row.setTotalPoints(studentTotalPoints);
            row.setTotalPossiblePoints(totalPossiblePoints);

            String ptsDisplay;
            if ("BOTH".equals(activityMode)) {
                if (isPenalized && awardPts > 0) {
                    ptsDisplay = "+" + awardPts + " / -" + penPts + " XP (" + (netPts >= 0 ? "+" : "") + netPts + " Net)";
                } else if (isPenalized) {
                    ptsDisplay = "-" + penPts + " XP";
                } else {
                    ptsDisplay = "+" + awardPts + " / " + totalPossiblePoints + " XP";
                }
            } else if (isVariableXp) {
                ptsDisplay = studentTotalPoints + " / " + totalPossiblePoints;
            } else if ("PENALTY".equals(activityMode)) {
                ptsDisplay = isPenalized ? ("-" + penPts + " XP") : "-";
            } else {
                ptsDisplay = null;
            }
            row.setPointsDisplay(ptsDisplay);
            row.setStatus(status);
            row.setAwardTimestamp(latestAwardedAt);
            row.setAwardedBy(latestAwardedBy);
            row.setRemarks(latestRemarks);
            row.setPenalized(isPenalized);
            row.setPenaltyXp(isPenalized ? penaltyXp : null);
            row.setPenaltyReason(isPenalized ? penaltyReason : null);
            row.setPenaltyRequestedBy(penaltyRequestedBy);
            row.setPenaltyApprovedBy(penaltyApprovedBy);
            row.setPenaltyRequestedAt(penaltyRequestedAt);
            row.setPenaltyApprovedAt(penaltyApprovedAt);
            row.setPenaltyStatus(penaltyStatusVal);

            // Populate Group / Team and Role (Captain / Vice Captain / Member)
            Team resolvedTeam = studentToTeamMap.get(s.getId());
            if (resolvedTeam == null && s.getTeam() != null) {
                resolvedTeam = s.getTeam();
            }
            if (resolvedTeam != null) {
                row.setTeamId(resolvedTeam.getId());
                row.setTeamName(resolvedTeam.getName());
                String r = studentToRoleMap.get(s.getId());
                if (r == null) {
                    boolean isCap = (resolvedTeam.getCaptain() != null && s.getId().equals(resolvedTeam.getCaptain().getId())) || s.isCaptain();
                    boolean isViceCap = (resolvedTeam.getViceCaptain() != null && s.getId().equals(resolvedTeam.getViceCaptain().getId()));
                    if (isCap) r = "Captain";
                    else if (isViceCap) r = "Vice Captain";
                    else r = "Member";
                }
                row.setTeamRole(r);
                row.setCaptain("Captain".equalsIgnoreCase(r));
                row.setViceCaptain("Vice Captain".equalsIgnoreCase(r));
            }

            allStudentRows.add(row);

            // Accumulate department stats
            if (s.getDepartment() != null) {
                Long dId = s.getDepartment().getId();
                String dName = s.getDepartment().getName();
                DeptStatAccumulator acc = deptStatsMap.computeIfAbsent(dId, k -> new DeptStatAccumulator(dId, dName));
                acc.totalStudents++;
                if (isPenalized) {
                    acc.penalizedCount++;
                }
                if ("PENALTY".equals(activityMode)) {
                    if (!isPenalized) {
                        acc.notAwardedCount++;
                    }
                } else if ("BOTH".equals(activityMode)) {
                    if (isFullyAwarded) acc.fullyAwardedCount++;
                    else if (isPartiallyAwarded) acc.partiallyAwardedCount++;
                    
                    if (isFullyAwarded || isPartiallyAwarded) acc.awardedCount++;
                    if (!isFullyAwarded && !isPartiallyAwarded && !isPenalized) {
                        acc.notAwardedCount++;
                    }
                } else {
                    if (isVariableXp || configuredCap > 1) {
                        if (isFullyAwarded) acc.fullyAwardedCount++;
                        else if (isPartiallyAwarded) acc.partiallyAwardedCount++;
                        else acc.notAwardedCount++;
                    } else {
                        if (isFullyAwarded) acc.awardedCount++;
                        else acc.notAwardedCount++;
                    }
                }
            }
        }

        // Summary metrics
        int totalStudents = allEligibleStudents.size();
        ReportSummaryDto summaryDto = new ReportSummaryDto();
        summaryDto.setTotalStudents(totalStudents);
        summaryDto.setVariableXp(isVariableXp);
        summaryDto.setCapLimit(configuredCap);
        summaryDto.setActivityMode(activityMode);
        summaryDto.setPenalizedCount(countPenalized);
        summaryDto.setPenalizedPercentage(totalStudents > 0
                ? Math.round((countPenalized * 100.0 / totalStudents) * 100.0) / 100.0
                : 0.0);

        List<CapSummaryMetricDto> capMetrics = new ArrayList<>();
        for (int k = 1; k <= configuredCap; k++) {
            int count = capAwardCounts[k];
            double pct = totalStudents > 0
                    ? Math.round((count * 100.0 / totalStudents) * 100.0) / 100.0
                    : 0.0;
            capMetrics.add(new CapSummaryMetricDto(k, "CAP " + k, count, pct));
        }
        summaryDto.setCapMetrics(capMetrics);
        summaryDto.setFullyCompletedAllCapsCount(countFullyAwarded);
        summaryDto.setFullyCompletedAllCapsPercentage(totalStudents > 0
                ? Math.round((countFullyAwarded * 100.0 / totalStudents) * 100.0) / 100.0
                : 0.0);

        if ("PENALTY".equals(activityMode)) {
            summaryDto.setAwardedCount(0);
            summaryDto.setAwardedPercentage(0.0);
            summaryDto.setNotAwardedCount(totalStudents - countPenalized);
            summaryDto.setNotAwardedPercentage(totalStudents > 0
                    ? Math.round(((totalStudents - countPenalized) * 100.0 / totalStudents) * 100.0) / 100.0
                    : 0.0);
        } else if ("BOTH".equals(activityMode)) {
            summaryDto.setAwardedCount(countFullyAwarded);
            summaryDto.setAwardedPercentage(totalStudents > 0
                    ? Math.round((countFullyAwarded * 100.0 / totalStudents) * 100.0) / 100.0
                    : 0.0);
            summaryDto.setNotAwardedCount(countNotAwarded);
            summaryDto.setNotAwardedPercentage(totalStudents > 0
                    ? Math.round((countNotAwarded * 100.0 / totalStudents) * 100.0) / 100.0
                    : 0.0);
            if (isVariableXp) {
                summaryDto.setFullyAwardedCount(countFullyAwarded);
                summaryDto.setPartiallyAwardedCount(countPartiallyAwarded);
                summaryDto.setFullyAwardedPercentage(totalStudents > 0
                        ? Math.round((countFullyAwarded * 100.0 / totalStudents) * 100.0) / 100.0
                        : 0.0);
                summaryDto.setPartiallyAwardedPercentage(totalStudents > 0
                        ? Math.round((countPartiallyAwarded * 100.0 / totalStudents) * 100.0) / 100.0
                        : 0.0);
            }
        } else if (isVariableXp) {
            summaryDto.setFullyAwardedCount(countFullyAwarded);
            summaryDto.setPartiallyAwardedCount(countPartiallyAwarded);
            summaryDto.setNotAwardedCount(countNotAwarded);
            summaryDto.setFullyAwardedPercentage(totalStudents > 0
                    ? Math.round((countFullyAwarded * 100.0 / totalStudents) * 100.0) / 100.0
                    : 0.0);
            summaryDto.setPartiallyAwardedPercentage(totalStudents > 0
                    ? Math.round((countPartiallyAwarded * 100.0 / totalStudents) * 100.0) / 100.0
                    : 0.0);
        } else {
            summaryDto.setAwardedCount(countFullyAwarded);
            summaryDto.setNotAwardedCount(countNotAwarded);
            summaryDto.setPartiallyAwardedCount(countPartiallyAwarded);
            summaryDto.setAwardedPercentage(totalStudents > 0
                    ? Math.round((countFullyAwarded * 100.0 / totalStudents) * 100.0) / 100.0
                    : 0.0);
            summaryDto.setPartiallyAwardedPercentage(totalStudents > 0
                    ? Math.round((countPartiallyAwarded * 100.0 / totalStudents) * 100.0) / 100.0
                    : 0.0);
            summaryDto.setNotAwardedPercentage(totalStudents > 0
                    ? Math.round((countNotAwarded * 100.0 / totalStudents) * 100.0) / 100.0
                    : 0.0);
        }

        // Department-wise summary (only if Department is All)
        List<DepartmentReportSummaryDto> deptSummaryList = new ArrayList<>();
        if (departmentId == null || departmentId == 0) {
            for (DeptStatAccumulator acc : deptStatsMap.values()) {
                if (acc.totalStudents > 0) {
                    DepartmentReportSummaryDto dDto = new DepartmentReportSummaryDto();
                    dDto.setDepartmentId(acc.departmentId);
                    dDto.setDepartmentName(acc.departmentName);
                    dDto.setTotalStudents(acc.totalStudents);
                    dDto.setPenalizedCount(acc.penalizedCount);
                    dDto.setPenalizedPercentage(acc.totalStudents > 0
                            ? Math.round((acc.penalizedCount * 100.0 / acc.totalStudents) * 100.0) / 100.0
                            : 0.0);
                    if ("PENALTY".equals(activityMode)) {
                        dDto.setAwardedCount(0);
                        dDto.setAwardedPercentage(0.0);
                        dDto.setNotAwardedCount(acc.totalStudents - acc.penalizedCount);
                    } else {
                        int deptAwarded = (isVariableXp || configuredCap > 1)
                                ? (acc.fullyAwardedCount + acc.partiallyAwardedCount)
                                : acc.awardedCount;
                        dDto.setAwardedCount(deptAwarded);
                        dDto.setNotAwardedCount(acc.notAwardedCount);
                        dDto.setAwardedPercentage(acc.totalStudents > 0
                                ? Math.round((deptAwarded * 100.0 / acc.totalStudents) * 100.0) / 100.0
                                : 0.0);
                        dDto.setFullyAwardedCount(acc.fullyAwardedCount);
                        dDto.setPartiallyAwardedCount(acc.partiallyAwardedCount);
                        dDto.setFullyAwardedPercentage(acc.totalStudents > 0
                                ? Math.round((acc.fullyAwardedCount * 100.0 / acc.totalStudents) * 100.0) / 100.0
                                : 0.0);
                    }
                    deptSummaryList.add(dDto);
                }
            }
            deptSummaryList.sort(Comparator.comparing(DepartmentReportSummaryDto::getDepartmentName, String.CASE_INSENSITIVE_ORDER));
        }

        // Ensure student rows are strictly sorted Department-wise -> Class/Section-wise -> (Team-wise if group activity) -> Role-wise -> Student Name-wise
        allStudentRows.sort((r1, r2) -> {
            String d1 = r1.getDepartment() != null ? r1.getDepartment().trim().toLowerCase() : "";
            String d2 = r2.getDepartment() != null ? r2.getDepartment().trim().toLowerCase() : "";
            int dComp = d1.compareTo(d2);
            if (dComp != 0) return dComp;

            String s1 = r1.getSection() != null ? r1.getSection().trim().toLowerCase() : "";
            String s2 = r2.getSection() != null ? r2.getSection().trim().toLowerCase() : "";
            int sComp = s1.compareTo(s2);
            if (sComp != 0) return sComp;

            if (isGroupActivity) {
                String t1 = r1.getTeamName() != null ? r1.getTeamName().trim().toLowerCase() : "\uffff";
                String t2 = r2.getTeamName() != null ? r2.getTeamName().trim().toLowerCase() : "\uffff";
                int tComp = t1.compareTo(t2);
                if (tComp != 0) return tComp;

                int roleOrder1 = r1.isCaptain() ? 1 : (r1.isViceCaptain() ? 2 : (r1.getTeamRole() != null ? 3 : 4));
                int roleOrder2 = r2.isCaptain() ? 1 : (r2.isViceCaptain() ? 2 : (r2.getTeamRole() != null ? 3 : 4));
                if (roleOrder1 != roleOrder2) return Integer.compare(roleOrder1, roleOrder2);
            }

            String n1 = r1.getStudentName() != null ? r1.getStudentName().trim().toLowerCase() : "";
            String n2 = r2.getStudentName() != null ? r2.getStudentName().trim().toLowerCase() : "";
            int nComp = n1.compareTo(n2);
            if (nComp != 0) return nComp;

            String reg1 = r1.getRegNo() != null ? r1.getRegNo().trim() : (r1.getSprNo() != null ? r1.getSprNo().trim() : "");
            String reg2 = r2.getRegNo() != null ? r2.getRegNo().trim() : (r2.getSprNo() != null ? r2.getSprNo().trim() : "");
            return reg1.compareToIgnoreCase(reg2);
        });

        // Filter by student search query if provided
        List<StudentReportRowDto> searchedRows = allStudentRows;
        if (search != null && !search.trim().isEmpty()) {
            String q = search.trim().toLowerCase();
            searchedRows = allStudentRows.stream().filter(r ->
                    r.getStudentName().toLowerCase().contains(q) ||
                    r.getRegNo().toLowerCase().contains(q) ||
                    r.getSprNo().toLowerCase().contains(q) ||
                    (r.getTeamName() != null && r.getTeamName().toLowerCase().contains(q))
            ).collect(Collectors.toList());
        }

        // Pagination
        int safePage = Math.max(0, page);
        int safeSize = (size <= 0) ? 50 : Math.min(size, 20000);
        int totalFiltered = searchedRows.size();
        int totalPages = totalFiltered > 0 ? (int) Math.ceil((double) totalFiltered / safeSize) : 0;
        int fromIndex = safePage * safeSize;
        List<StudentReportRowDto> paginatedRows;

        if (fromIndex >= totalFiltered) {
            paginatedRows = Collections.emptyList();
        } else {
            int toIndex = Math.min(fromIndex + safeSize, totalFiltered);
            paginatedRows = searchedRows.subList(fromIndex, toIndex);
        }

        ActivityPointsReportResponse response = new ActivityPointsReportResponse();
        response.setActivityDetails(actDetailDto);
        response.setSummary(summaryDto);
        response.setDepartmentSummary(deptSummaryList);
        response.setCapColumns(capColumns);
        response.setStudents(paginatedRows);
        response.setTotalStudentsCount(totalFiltered);
        response.setPage(safePage);
        response.setSize(safeSize);
        response.setTotalPages(totalPages);
        response.setActiveTimeFilterLabel(timeFilterLabel);

        return response;
    }

    private AcademicYear resolveEffectiveAcademicYear(User currentUser, String requestedYear) {
        if (currentUser != null && !authUtils.isSuperAdmin(currentUser)) {
            // Admin (Year Admin) is bound to their assigned academic year
            if (currentUser.getAcademicYear() != null) {
                return currentUser.getAcademicYear();
            }
            if (currentUser.getAssignedYear() != null && currentUser.getAssignedYear().getYearNo() != null) {
                byte yNo = currentUser.getAssignedYear().getYearNo();
                if (yNo == 1) return AcademicYear.FIRST_YEAR;
                if (yNo == 2) return AcademicYear.SECOND_YEAR;
                if (yNo == 3) return AcademicYear.THIRD_YEAR;
                if (yNo == 4) return AcademicYear.FOURTH_YEAR;
            }
        }
        return AcademicYear.fromString(requestedYear);
    }

    private List<Student> fetchEligibleStudents(
            AcademicYear academicYear,
            Long semesterId,
            Long departmentId,
            Long sectionId,
            Long stageId) {

        Integer targetStageOrder = null;
        if (stageId != null && stageId > 0 && activityStageRepository != null) {
            try {
                Optional<ActivityStage> stOpt = activityStageRepository.findById(stageId);
                targetStageOrder = stOpt.map(ActivityStage::getDisplayOrder).orElse(stageId.intValue());
            } catch (Exception e) {
                targetStageOrder = stageId.intValue();
            }
        }
        final Integer finalStageOrder = targetStageOrder;

        List<Student> allStudents = studentRepository.findAll();

        return allStudents.stream().filter(s -> {
            if (!s.isActive() || s.isDeleted()) {
                return false;
            }

            // Academic Year Filter
            if (academicYear != null) {
                AcademicYear sYear = AcademicYear.fromStudent(s);
                if (sYear != null && sYear != academicYear) {
                    return false;
                }
                if (sYear == null) {
                    String rawYear = s.getYear();
                    if (rawYear != null && !isYearStringMatching(academicYear, rawYear)) {
                        return false;
                    }
                }
            }

            // Department Filter
            if (departmentId != null && departmentId > 0) {
                if (s.getDepartment() == null || !departmentId.equals(s.getDepartment().getId())) {
                    return false;
                }
            }

            // Section Filter
            if (sectionId != null && sectionId > 0) {
                if (s.getSection() == null || !sectionId.equals(s.getSection().getId())) {
                    return false;
                }
            }

            // Semester Filter
            if (semesterId != null && semesterId > 0) {
                if (s.getSemesterRef() != null) {
                    if (!semesterId.equals(s.getSemesterRef().getId())) {
                        return false;
                    }
                } else if (s.getSemester() != null) {
                    try {
                        long semNo = Long.parseLong(s.getSemester().replaceAll("\\D", ""));
                        if (semNo != semesterId) return false;
                    } catch (Exception ignored) {
                    }
                }
            }

            // Stage Filter
            if (finalStageOrder != null) {
                if (s.getCurrentStage() != finalStageOrder && s.getStage() != finalStageOrder
                        && s.getCurrentStage() != stageId.intValue() && s.getStage() != stageId.intValue()) {
                    return false;
                }
            }

            return true;
        }).sorted((s1, s2) -> {
            // 1. Department wise (A-Z)
            String d1 = (s1.getDepartment() != null && s1.getDepartment().getName() != null)
                    ? s1.getDepartment().getName().trim().toLowerCase() : "";
            String d2 = (s2.getDepartment() != null && s2.getDepartment().getName() != null)
                    ? s2.getDepartment().getName().trim().toLowerCase() : "";
            int dComp = d1.compareTo(d2);
            if (dComp != 0) return dComp;

            // 2. Class / Section wise (A-Z)
            String sec1 = (s1.getSection() != null && s1.getSection().getSectionName() != null)
                    ? s1.getSection().getSectionName().trim().toLowerCase() : "";
            String sec2 = (s2.getSection() != null && s2.getSection().getSectionName() != null)
                    ? s2.getSection().getSectionName().trim().toLowerCase() : "";
            int secComp = sec1.compareTo(sec2);
            if (secComp != 0) return secComp;

            // 3. Student Name wise (A-Z)
            String n1 = s1.getFullName() != null ? s1.getFullName().trim().toLowerCase() : "";
            String n2 = s2.getFullName() != null ? s2.getFullName().trim().toLowerCase() : "";
            int comp = n1.compareTo(n2);
            if (comp != 0) return comp;

            // 4. Reg No / SPR No
            String r1 = s1.getRegNo() != null ? s1.getRegNo().trim() : (s1.getSprNo() != null ? s1.getSprNo().trim() : "");
            String r2 = s2.getRegNo() != null ? s2.getRegNo().trim() : (s2.getSprNo() != null ? s2.getSprNo().trim() : "");
            return r1.compareToIgnoreCase(r2);
        }).collect(Collectors.toList());
    }

    private boolean isYearStringMatching(AcademicYear targetYear, String studentYear) {
        if (targetYear == null || studentYear == null) return false;
        String y = studentYear.trim().toUpperCase();
        switch (targetYear) {
            case FIRST_YEAR:
                return y.contains("1") || y.contains("FIRST") || y.equals("I");
            case SECOND_YEAR:
                return y.contains("2") || y.contains("SECOND") || y.equals("II");
            case THIRD_YEAR:
                return y.contains("3") || y.contains("THIRD") || y.equals("III");
            case FOURTH_YEAR:
                return y.contains("4") || y.contains("FOURTH") || y.equals("IV");
            default:
                return false;
        }
    }

    private boolean isAssignmentYearMatching(String assignYear, AcademicYear targetYear, String studentYear) {
        if (assignYear == null || assignYear.trim().isEmpty()) {
            return true;
        }
        AcademicYear aYear = AcademicYear.fromString(assignYear);
        if (aYear != null && targetYear != null && aYear == targetYear) {
            return true;
        }
        if (targetYear != null && isYearStringMatching(targetYear, assignYear)) {
            return true;
        }
        if (studentYear != null && isYearStringMatching(AcademicYear.fromString(studentYear), assignYear)) {
            return true;
        }
        String ay = assignYear.trim().toLowerCase();
        String sy = (studentYear != null ? studentYear.trim().toLowerCase() : "");
        return ay.equals(sy) || (ay.contains("1") && (sy.contains("1") || sy.contains("first") || sy.equals("i")));
    }

    private boolean isStudentAssignedToActivity(
            Student s,
            AcademicYear effectiveAcademicYear,
            Integer finalStageOrder,
            Long stageId,
            ActivityAssignment a) {

        // 1. Stage restriction on assignment
        if (stageId != null && stageId > 0 && a.getStage() != null) {
            boolean stageMatches = a.getStage().getId().equals(stageId)
                    || (finalStageOrder != null && a.getStage().getDisplayOrder() == finalStageOrder.intValue());
            if (!stageMatches) {
                return false;
            }
        }

        // 2. Year restriction on assignment
        if (a.getYear() != null && !a.getYear().trim().isEmpty()) {
            boolean yearMatches = isAssignmentYearMatching(a.getYear(), effectiveAcademicYear, s.getYear());
            if (!yearMatches) {
                return false;
            }
        }

        // 3. Department and Section restriction
        Long studentDeptId = s.getDepartment() != null ? s.getDepartment().getId()
                : (s.getSection() != null && s.getSection().getDepartment() != null ? s.getSection().getDepartment().getId() : null);
        Long studentSecId = s.getSection() != null ? s.getSection().getId() : null;

        Long assignDeptId = a.getDepartment() != null ? a.getDepartment().getId() : null;
        Long assignSecId = a.getSection() != null ? a.getSection().getId() : null;
        AssignmentScope scope = a.getAssignmentScope();

        if (scope == AssignmentScope.GLOBAL) {
            if (assignDeptId == null && assignSecId == null) {
                return true;
            }
            if (assignSecId != null) {
                return studentSecId != null && assignSecId.equals(studentSecId);
            }
            if (assignDeptId != null) {
                return studentDeptId != null && assignDeptId.equals(studentDeptId);
            }
            return true;
        } else if (scope == AssignmentScope.SECTION || assignSecId != null) {
            if (assignSecId != null) {
                return studentSecId != null && assignSecId.equals(studentSecId);
            }
            if (assignDeptId != null) {
                return studentDeptId != null && assignDeptId.equals(studentDeptId);
            }
            return false;
        } else if (scope == AssignmentScope.DEPARTMENT || assignDeptId != null) {
            if (assignDeptId != null) {
                if (studentDeptId == null || !assignDeptId.equals(studentDeptId)) {
                    return false;
                }
                if (assignSecId != null) {
                    return studentSecId != null && assignSecId.equals(studentSecId);
                }
                return true;
            }
            return false;
        } else if (scope == AssignmentScope.SPECIFIC_FACULTY) {
            if (assignSecId != null) {
                return studentSecId != null && assignSecId.equals(studentSecId);
            }
            if (assignDeptId != null) {
                return studentDeptId != null && assignDeptId.equals(studentDeptId);
            }
            return true;
        }

        // Default fallback if scope is null
        if (assignSecId != null) {
            return studentSecId != null && assignSecId.equals(studentSecId);
        }
        if (assignDeptId != null) {
            return studentDeptId != null && assignDeptId.equals(studentDeptId);
        }
        return true;
    }

    private String resolveStaffForParticularClass(Long activityId, Long deptId, Long secId, List<StudentActivityXp> awardsInWindow) {
        // 1. Look for awards in window for this class
        for (StudentActivityXp axp : awardsInWindow) {
            if (axp.getStudent() != null &&
                    axp.getStudent().getDepartment() != null && deptId.equals(axp.getStudent().getDepartment().getId()) &&
                    axp.getStudent().getSection() != null && secId.equals(axp.getStudent().getSection().getId())) {
                if (axp.getTeacher() != null && axp.getTeacher().getFullName() != null) {
                    return axp.getTeacher().getFullName();
                }
            }
        }

        // 2. Look for assigned faculty in ActivityAssignment
        try {
            List<ActivityAssignment> assignments = activityAssignmentRepository.findByActivityId(activityId);
            for (ActivityAssignment a : assignments) {
                if (a.getTeacher() != null && a.getTeacher().getFullName() != null) {
                    if (a.getDepartment() != null && deptId.equals(a.getDepartment().getId())) {
                        if (a.getSection() != null && secId.equals(a.getSection().getId())) {
                            return a.getTeacher().getFullName();
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Could not query activity assignment for staff resolution: {}", e.getMessage());
        }

        return "-";
    }

    public List<ActivityPointsReportResponse> generateAllActivitiesReport(
            String academicYear,
            Long semesterId,
            Long departmentId,
            Long sectionId,
            Long stageId,
            Long subgroupId,
            LocalDate date,
            Integer month,
            Integer year,
            Integer weekNumber,
            String search) {

        List<Activity> targetActivities;
        if (stageId != null && stageId > 0) {
            targetActivities = activityRepository.findByStageId(stageId);
        } else {
            targetActivities = activityRepository.findAll();
        }

        if (subgroupId != null && subgroupId > 0) {
            targetActivities = targetActivities.stream()
                    .filter(a -> a.getSubgroup() != null && subgroupId.equals(a.getSubgroup().getId()))
                    .collect(Collectors.toList());
        }

        // Exclude deleted activities
        targetActivities = targetActivities.stream()
                .filter(a -> !a.isDeleted())
                .collect(Collectors.toList());

        List<ActivityPointsReportResponse> results = new ArrayList<>();
        for (Activity act : targetActivities) {
            try {
                ActivityPointsReportResponse res = generateActivityPointsReport(
                        academicYear, semesterId, departmentId, sectionId,
                        act.getStage() != null ? act.getStage().getId() : stageId,
                        act.getSubgroup() != null ? act.getSubgroup().getId() : subgroupId,
                        act.getId(), date, month, year, weekNumber, search, 0, 20000);
                // Retain display pageSize 50 and calculate totalPages so UI pagination works seamlessly
                res.setSize(50);
                res.setTotalPages(res.getTotalStudentsCount() > 0 ? (int) Math.ceil((double) res.getTotalStudentsCount() / 50) : 0);
                results.add(res);
            } catch (Exception ex) {
                log.error("Failed to generate report for activity {}: {}", act.getId(), ex.getMessage());
            }
        }
        return results;
    }

    private static class DeptStatAccumulator {
        Long departmentId;
        String departmentName;
        int totalStudents = 0;
        int awardedCount = 0;
        int notAwardedCount = 0;
        int fullyAwardedCount = 0;
        int partiallyAwardedCount = 0;
        int penalizedCount = 0;

        DeptStatAccumulator(Long departmentId, String departmentName) {
            this.departmentId = departmentId;
            this.departmentName = departmentName;
        }
    }
}
