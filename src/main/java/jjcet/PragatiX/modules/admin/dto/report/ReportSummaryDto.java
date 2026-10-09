package jjcet.PragatiX.modules.admin.dto.report;

import java.util.ArrayList;
import java.util.List;

public class ReportSummaryDto {
    private int totalStudents;
    private boolean isVariableXp;
    private int capLimit = 1;

    // Fixed XP Metrics
    private int awardedCount;
    private double awardedPercentage;
    private int notAwardedCount;
    private double notAwardedPercentage;

    // Variable XP Metrics
    private int fullyAwardedCount;
    private double fullyAwardedPercentage;
    private int partiallyAwardedCount;
    private double partiallyAwardedPercentage;

    // CAP-wise Breakdown Metrics (for multi-cap activities)
    private List<CapSummaryMetricDto> capMetrics = new ArrayList<>();
    private int fullyCompletedAllCapsCount;
    private double fullyCompletedAllCapsPercentage;

    // Penalty Metrics
    private int penalizedCount;
    private double penalizedPercentage;
    private String activityMode = "AWARD"; // "AWARD", "PENALTY", "BOTH"

    public ReportSummaryDto() {
    }

    public int getTotalStudents() {
        return totalStudents;
    }

    public void setTotalStudents(int totalStudents) {
        this.totalStudents = totalStudents;
    }

    public boolean isVariableXp() {
        return isVariableXp;
    }

    public void setVariableXp(boolean variableXp) {
        isVariableXp = variableXp;
    }

    public int getCapLimit() {
        return capLimit;
    }

    public void setCapLimit(int capLimit) {
        this.capLimit = capLimit;
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

    public double getNotAwardedPercentage() {
        return notAwardedPercentage;
    }

    public void setNotAwardedPercentage(double notAwardedPercentage) {
        this.notAwardedPercentage = notAwardedPercentage;
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
        return partiallyAwardedPercentage;
    }

    public void setPartiallyAwardedPercentage(double partiallyAwardedPercentage) {
        this.partiallyAwardedPercentage = partiallyAwardedPercentage;
    }

    public List<CapSummaryMetricDto> getCapMetrics() {
        return capMetrics;
    }

    public void setCapMetrics(List<CapSummaryMetricDto> capMetrics) {
        this.capMetrics = capMetrics;
    }

    public int getFullyCompletedAllCapsCount() {
        return fullyCompletedAllCapsCount;
    }

    public void setFullyCompletedAllCapsCount(int fullyCompletedAllCapsCount) {
        this.fullyCompletedAllCapsCount = fullyCompletedAllCapsCount;
    }

    public double getFullyCompletedAllCapsPercentage() {
        return fullyCompletedAllCapsPercentage;
    }

    public void setFullyCompletedAllCapsPercentage(double fullyCompletedAllCapsPercentage) {
        this.fullyCompletedAllCapsPercentage = fullyCompletedAllCapsPercentage;
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

    public String getActivityMode() {
        return activityMode;
    }

    public void setActivityMode(String activityMode) {
        this.activityMode = activityMode;
    }
}
