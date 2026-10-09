package jjcet.PragatiX.modules.admin.dto.report;

public class ActivityReportDetailDto {
    private Long id;
    private String activityName;
    private String description;
    private String frequency;
    private String frequencyNormalized; // ONE_TIME, DAILY, WEEKLY, MONTHLY
    private String awardType; // Fixed XP or Variable XP
    private boolean isVariableXp;
    private int pointsPerCap;
    private int capLimit;
    private int totalPossiblePoints;
    private String awardedByStaff; // "-" if All Departments, or actual staff name
    private String stageName;
    private String subgroupName;
    private Boolean awardEnabled = true;
    private Boolean penaltyEnabled = false;
    private Integer awardXp = 0;
    private Integer penaltyXp = 0;
    private String activityMode = "AWARD"; // "AWARD", "PENALTY", "BOTH"
    private Boolean isGroupActivity = false;

    public ActivityReportDetailDto() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getActivityName() {
        return activityName;
    }

    public void setActivityName(String activityName) {
        this.activityName = activityName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    public String getFrequencyNormalized() {
        return frequencyNormalized;
    }

    public void setFrequencyNormalized(String frequencyNormalized) {
        this.frequencyNormalized = frequencyNormalized;
    }

    public String getAwardType() {
        return awardType;
    }

    public void setAwardType(String awardType) {
        this.awardType = awardType;
    }

    public boolean isVariableXp() {
        return isVariableXp;
    }

    public void setVariableXp(boolean variableXp) {
        isVariableXp = variableXp;
    }

    public int getPointsPerCap() {
        return pointsPerCap;
    }

    public void setPointsPerCap(int pointsPerCap) {
        this.pointsPerCap = pointsPerCap;
    }

    public int getCapLimit() {
        return capLimit;
    }

    public void setCapLimit(int capLimit) {
        this.capLimit = capLimit;
    }

    public int getTotalPossiblePoints() {
        return totalPossiblePoints;
    }

    public void setTotalPossiblePoints(int totalPossiblePoints) {
        this.totalPossiblePoints = totalPossiblePoints;
    }

    public String getAwardedByStaff() {
        return awardedByStaff;
    }

    public void setAwardedByStaff(String awardedByStaff) {
        this.awardedByStaff = awardedByStaff;
    }

    public String getStageName() {
        return stageName;
    }

    public void setStageName(String stageName) {
        this.stageName = stageName;
    }

    public String getSubgroupName() {
        return subgroupName;
    }

    public void setSubgroupName(String subgroupName) {
        this.subgroupName = subgroupName;
    }

    public Boolean getAwardEnabled() {
        return awardEnabled;
    }

    public void setAwardEnabled(Boolean awardEnabled) {
        this.awardEnabled = awardEnabled;
    }

    public Boolean getPenaltyEnabled() {
        return penaltyEnabled;
    }

    public void setPenaltyEnabled(Boolean penaltyEnabled) {
        this.penaltyEnabled = penaltyEnabled;
    }

    public Integer getAwardXp() {
        return awardXp;
    }

    public void setAwardXp(Integer awardXp) {
        this.awardXp = awardXp;
    }

    public Integer getPenaltyXp() {
        return penaltyXp;
    }

    public void setPenaltyXp(Integer penaltyXp) {
        this.penaltyXp = penaltyXp;
    }

    public String getActivityMode() {
        return activityMode;
    }

    public void setActivityMode(String activityMode) {
        this.activityMode = activityMode;
    }

    public Boolean getIsGroupActivity() {
        return isGroupActivity;
    }

    public void setIsGroupActivity(Boolean isGroupActivity) {
        this.isGroupActivity = isGroupActivity;
    }
}
