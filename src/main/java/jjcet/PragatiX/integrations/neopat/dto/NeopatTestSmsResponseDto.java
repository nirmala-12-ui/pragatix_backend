package jjcet.PragatiX.integrations.neopat.dto;

public class NeopatTestSmsResponseDto {

    private boolean success;
    private String recipient;
    private String messageSid;
    private String details;

    public NeopatTestSmsResponseDto() {
    }

    public NeopatTestSmsResponseDto(boolean success, String recipient, String messageSid, String details) {
        this.success = success;
        this.recipient = recipient;
        this.messageSid = messageSid;
        this.details = details;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public String getMessageSid() {
        return messageSid;
    }

    public void setMessageSid(String messageSid) {
        this.messageSid = messageSid;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}
