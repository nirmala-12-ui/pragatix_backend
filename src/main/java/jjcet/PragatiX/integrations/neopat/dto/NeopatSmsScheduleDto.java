package jjcet.PragatiX.integrations.neopat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class NeopatSmsScheduleDto {

    @NotNull(message = "Enabled flag is required")
    private Boolean enabled;

    @NotBlank(message = "Day of week is required")
    @Pattern(regexp = "^(MONDAY|TUESDAY|WEDNESDAY|THURSDAY|FRIDAY|SATURDAY|SUNDAY)$", 
             message = "Day of week must be MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, or SUNDAY")
    private String dayOfWeek;

    @NotBlank(message = "Send time is required")
    @Pattern(regexp = "^([01]?[0-9]|2[0-3]):[0-5][0-9](:[0-5][0-9])?$", 
             message = "Send time must be in HH:mm:ss or HH:mm format")
    private String sendTime;

    public NeopatSmsScheduleDto() {
    }

    public NeopatSmsScheduleDto(Boolean enabled, String dayOfWeek, String sendTime) {
        this.enabled = enabled;
        this.dayOfWeek = dayOfWeek;
        this.sendTime = sendTime;
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
}
