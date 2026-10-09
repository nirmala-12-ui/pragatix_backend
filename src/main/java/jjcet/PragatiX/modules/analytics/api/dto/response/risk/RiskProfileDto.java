package jjcet.PragatiX.modules.analytics.api.dto.response.risk;

public record RiskProfileDto(
        Long studentId,
        String studentName,
        String regNo,
        String department,
        String section,
        String year,
        Double attendancePercentage,
        Long daysSinceLastAttendance,
        Long totalXp,
        Long penaltyXp,
        String riskLevel
) {
    public RiskProfileDto(
            Long studentId,
            String studentName,
            String department,
            Double attendancePercentage,
            Long daysSinceLastAttendance,
            Long totalXp,
            Long penaltyXp
    ) {
        this(studentId, studentName, "", department, "", "", attendancePercentage, daysSinceLastAttendance, totalXp, penaltyXp, "LOW");
    }
}