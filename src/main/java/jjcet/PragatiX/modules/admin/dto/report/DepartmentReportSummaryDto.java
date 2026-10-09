package jjcet.PragatiX.modules.admin.dto.report;

public class DepartmentReportSummaryDto {
    private Long departmentId;
    private String departmentName;
    private int totalStudents;

    // Fixed XP Metrics
    private int awardedCount;
    private double awardedPercentage;
    private int notAwardedCount;

    // Variable XP Metrics
    private int fullyAwardedCount;
    private double fullyAwardedPercentage;
    private int partiallyAwardedCount;

    // Penalty Metrics
    private int penalizedCount;
    private double penalizedPercentage;

    public DepartmentReportSummaryDto() {
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public int getTotalStudents() {
        return totalStudents;
    }

    public void setTotalStudents(int totalStudents) {
        this.totalStudents = totalStudents;
    }

    public int getAwardedCount() {
        return awardedCount;
    }

    public void setAwardedCount(int awardedCount) {
        this.awardedCount = awardedCount;
    }

    public double getAwardedPercentage() {
        return awardedPercentage;
    }

    public void setAwardedPercentage(double awardedPercentage) {
        this.awardedPercentage = awardedPercentage;
    }

    public int getNotAwardedCount() {
        return notAwardedCount;
    }

    public void setNotAwardedCount(int notAwardedCount) {
        this.notAwardedCount = notAwardedCount;
    }

    public int getFullyAwardedCount() {
        return fullyAwardedCount;
    }

    public void setFullyAwardedCount(int fullyAwardedCount) {
        this.fullyAwardedCount = fullyAwardedCount;
    }

    public double getFullyAwardedPercentage() {
        return fullyAwardedPercentage;
    }

    public void setFullyAwardedPercentage(double fullyAwardedPercentage) {
        this.fullyAwardedPercentage = fullyAwardedPercentage;
    }

    public int getPartiallyAwardedCount() {
        return partiallyAwardedCount;
    }

    public void setPartiallyAwardedCount(int partiallyAwardedCount) {
        this.partiallyAwardedCount = partiallyAwardedCount;
    }

    public double getPartiallyAwardedPercentage() {
        return totalStudents > 0
                ? Math.round((partiallyAwardedCount * 100.0 / totalStudents) * 100.0) / 100.0
                : 0.0;
    }

    public double getNotAwardedPercentage() {
        return totalStudents > 0
                ? Math.round((notAwardedCount * 100.0 / totalStudents) * 100.0) / 100.0
                : 0.0;
    }

    public int getPenalizedCount() {
        return penalizedCount;
    }

    public void setPenalizedCount(int penalizedCount) {
        this.penalizedCount = penalizedCount;
    }

    public double getPenalizedPercentage() {
        return penalizedPercentage;
    }

    public void setPenalizedPercentage(double penalizedPercentage) {
        this.penalizedPercentage = penalizedPercentage;
    }
}
