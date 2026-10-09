package jjcet.PragatiX.integrations.neopat.dto;

public class NeopatTestSmsRequestDto {

    private String phoneNumber;
    private String testMessage;

    public NeopatTestSmsRequestDto() {
    }

    public NeopatTestSmsRequestDto(String phoneNumber, String testMessage) {
        this.phoneNumber = phoneNumber;
        this.testMessage = testMessage;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getTestMessage() {
        return testMessage;
    }

    public void setTestMessage(String testMessage) {
        this.testMessage = testMessage;
    }
}
