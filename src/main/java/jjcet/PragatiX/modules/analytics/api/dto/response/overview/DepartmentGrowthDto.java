package jjcet.PragatiX.modules.analytics.api.dto.response.overview;

public record DepartmentGrowthDto(
        Long departmentId,
        String departmentName,
        Double growthRate,
        Long totalXp,
        Double attendanceRate,
        Double xpShare,
        Long studentCount
) {
    public DepartmentGrowthDto(Long departmentId, String departmentName, Double growthRate, Long totalXp) {
        this(departmentId, departmentName, growthRate, totalXp, null, null, null);
    }

    public DepartmentGrowthDto(Long departmentId, String departmentName, Double growthRate, Long totalXp, Double attendanceRate, Double xpShare) {
        this(departmentId, departmentName, growthRate, totalXp, attendanceRate, xpShare, null);
    }
}