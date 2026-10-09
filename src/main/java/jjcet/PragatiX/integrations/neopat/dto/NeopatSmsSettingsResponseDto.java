package jjcet.PragatiX.integrations.neopat.dto;

public class NeopatSmsSettingsResponseDto {

    private Long id;
    private Boolean enabled;
    private String dayOfWeek;
    private String sendTime;
    private String timezone;
    private String updatedAt;
    private String updatedBy;

    public NeopatSmsSettingsResponseDto() {
    }

    public NeopatSmsSettingsResponseDto(Long id, Boolean enabled, String dayOfWeek, String sendTime, String timezone, String updatedAt, String updatedBy) {
        this.id = id;
        this.enabled = enabled;
        this.dayOfWeek = dayOfWeek;
        this.sendTime = sendTime;
        this.timezone = timezone;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
}
