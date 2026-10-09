package jjcet.PragatiX.modules.admin.dto.report;

import java.util.List;

public class StudentReportRowDto {
    private Long id;
    private String sprNo;
    private String regNo;
    private String studentName;
    private String department;
    private String section;
    private String stage;
    private String subgroup;

    // List of dynamic CAP awards corresponding to capColumns
    private List<StudentCapAwardDto> capAwards;

    // Points & Status
    private Integer totalPoints;
    private Integer totalPossiblePoints;
    private Integer awardPoints;
    private Integer netPoints;
    private String pointsDisplay; // e.g. "75 / 100"
    private String status; // "Awarded", "Partial Awarded", "Not Awarded"

    // Latest award details (used especially for One-Time / Single CAP)
    private String awardTimestamp;
    private String awardedBy;
    private String remarks;

    // Penalty status
    private boolean isPenalized = false;
    private Integer penaltyXp;
    private String penaltyReason;
    private String penaltyRequestedBy;
    private String penaltyApprovedBy;
    private String penaltyRequestedAt;
    private String penaltyApprovedAt;
    private String penaltyStatus;

    // Group / Team Information
    private Long teamId;
    private String teamName;
    private String teamRole; // "Captain", "Vice Captain", "Member"
    private boolean isCaptain = false;
    private boolean isViceCaptain = false;

    public StudentReportRowDto() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSprNo() {
        return sprNo;
    }

    public void setSprNo(String sprNo) {
        this.sprNo = sprNo;
    }

    public String getRegNo() {
        return regNo;
    }

    public void setRegNo(String regNo) {
        this.regNo = regNo;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public String getStage() {
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }

    public String getSubgroup() {
        return subgroup;
    }

    public void setSubgroup(String subgroup) {
        this.subgroup = subgroup;
    }

    public List<StudentCapAwardDto> getCapAwards() {
        return capAwards;
    }

    public void setCapAwards(List<StudentCapAwardDto> capAwards) {
        this.capAwards = capAwards;
    }

    public Integer getTotalPoints() {
        return totalPoints;
    }

    public void setTotalPoints(Integer totalPoints) {
        this.totalPoints = totalPoints;
    }

    public Integer getTotalPossiblePoints() {
        return totalPossiblePoints;
    }

    public void setTotalPossiblePoints(Integer totalPossiblePoints) {
        this.totalPossiblePoints = totalPossiblePoints;
    }

    public String getPointsDisplay() {
        return pointsDisplay;
    }

    public void setPointsDisplay(String pointsDisplay) {
        this.pointsDisplay = pointsDisplay;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAwardTimestamp() {
        return awardTimestamp;
    }

    public void setAwardTimestamp(String awardTimestamp) {
        this.awardTimestamp = awardTimestamp;
    }

    public String getAwardedBy() {
        return awardedBy;
    }

    public void setAwardedBy(String awardedBy) {
        this.awardedBy = awardedBy;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public boolean isPenalized() {
        return isPenalized;
    }

    public void setPenalized(boolean penalized) {
        this.isPenalized = penalized;
    }

    public Integer getPenaltyXp() {
        return penaltyXp;
    }

    public void setPenaltyXp(Integer penaltyXp) {
        this.penaltyXp = penaltyXp;
    }

    public String getPenaltyReason() {
        return penaltyReason;
    }

    public void setPenaltyReason(String penaltyReason) {
        this.penaltyReason = penaltyReason;
    }

    public String getPenaltyRequestedBy() {
        return penaltyRequestedBy;
    }

    public void setPenaltyRequestedBy(String penaltyRequestedBy) {
        this.penaltyRequestedBy = penaltyRequestedBy;
    }

    public String getPenaltyApprovedBy() {
        return penaltyApprovedBy;
    }

    public void setPenaltyApprovedBy(String penaltyApprovedBy) {
        this.penaltyApprovedBy = penaltyApprovedBy;
    }

    public String getPenaltyRequestedAt() {
        return penaltyRequestedAt;
    }

    public void setPenaltyRequestedAt(String penaltyRequestedAt) {
        this.penaltyRequestedAt = penaltyRequestedAt;
    }

    public String getPenaltyApprovedAt() {
        return penaltyApprovedAt;
    }

    public void setPenaltyApprovedAt(String penaltyApprovedAt) {
        this.penaltyApprovedAt = penaltyApprovedAt;
    }

    public String getPenaltyStatus() {
        return penaltyStatus;
    }

    public void setPenaltyStatus(String penaltyStatus) {
        this.penaltyStatus = penaltyStatus;
    }

    public Integer getAwardPoints() {
        return awardPoints;
    }

    public void setAwardPoints(Integer awardPoints) {
        this.awardPoints = awardPoints;
    }

    public Integer getNetPoints() {
        return netPoints;
    }

    public void setNetPoints(Integer netPoints) {
        this.netPoints = netPoints;
    }

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public String getTeamRole() {
        return teamRole;
    }

    public void setTeamRole(String teamRole) {
        this.teamRole = teamRole;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("isCaptain")
    public boolean isCaptain() {
        return isCaptain;
    }

    public void setCaptain(boolean captain) {
        isCaptain = captain;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("isViceCaptain")
    public boolean isViceCaptain() {
        return isViceCaptain;
    }

    public void setViceCaptain(boolean viceCaptain) {
        isViceCaptain = viceCaptain;
    }
}
