package jjcet.PragatiX.integrations.neopat.dto;

import java.io.Serializable;

public class NeopatBatchSmsConfigDto implements Serializable {

    private Integer batchYear;
    private Long scheduleId;
    private Boolean enabled;
    private String dayOfWeek;
    private String sendTime;
    private String timezone;
    private String updatedAt;
    private String updatedBy;
    private String nextExecution;
    private String lastExecutionStatus;
    private String lastExecutionTime;
    private Long eligibleAssessmentCount;
    private Long processedAssessmentCount;

    public NeopatBatchSmsConfigDto() {
    }

    public NeopatBatchSmsConfigDto(Integer batchYear, Long scheduleId, Boolean enabled, String dayOfWeek,
                                   String sendTime, String timezone, String updatedAt, String updatedBy,
                                   String nextExecution, String lastExecutionStatus, String lastExecutionTime,
                                   Long eligibleAssessmentCount, Long processedAssessmentCount) {
        this.batchYear = batchYear;
        this.scheduleId = scheduleId;
        this.enabled = enabled;
        this.dayOfWeek = dayOfWeek;
        this.sendTime = sendTime;
        this.timezone = timezone;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
        this.nextExecution = nextExecution;
        this.lastExecutionStatus = lastExecutionStatus;
        this.lastExecutionTime = lastExecutionTime;
        this.eligibleAssessmentCount = eligibleAssessmentCount;
        this.processedAssessmentCount = processedAssessmentCount;
    }

    public Integer getBatchYear() {
        return batchYear;
    }

    public void setBatchYear(Integer batchYear) {
        this.batchYear = batchYear;
    }

    public Long getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(Long scheduleId) {
        this.scheduleId = scheduleId;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(String dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public String getSendTime() {
        return sendTime;
    }

    public void setSendTime(String sendTime) {
        this.sendTime = sendTime;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public String getNextExecution() {
        return nextExecution;
    }

    public void setNextExecution(String nextExecution) {
        this.nextExecution = nextExecution;
    }

    public String getLastExecutionStatus() {
        return lastExecutionStatus;
    }

    public void setLastExecutionStatus(String lastExecutionStatus) {
        this.lastExecutionStatus = lastExecutionStatus;
    }

    public String getLastExecutionTime() {
        return lastExecutionTime;
    }

    public void setLastExecutionTime(String lastExecutionTime) {
        this.lastExecutionTime = lastExecutionTime;
    }

    public Long getEligibleAssessmentCount() {
        return eligibleAssessmentCount;
    }

    public void setEligibleAssessmentCount(Long eligibleAssessmentCount) {
        this.eligibleAssessmentCount = eligibleAssessmentCount;
    }

    public Long getProcessedAssessmentCount() {
        return processedAssessmentCount;
    }

    public void setProcessedAssessmentCount(Long processedAssessmentCount) {
        this.processedAssessmentCount = processedAssessmentCount;
    }
}
