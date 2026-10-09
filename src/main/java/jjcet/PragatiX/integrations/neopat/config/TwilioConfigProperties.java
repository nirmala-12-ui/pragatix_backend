package jjcet.PragatiX.integrations.neopat.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "twilio")
public class TwilioConfigProperties {

    private String accountSid;
    private String authToken;
    private String fromNumber;
    private String testPhoneNumber;

    public String getAccountSid() {
        return accountSid;
    }

    public void setAccountSid(String accountSid) {
        this.accountSid = accountSid;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public String getFromNumber() {
        return fromNumber;
    }

    public void setFromNumber(String fromNumber) {
        this.fromNumber = fromNumber;
    }

    public String getTestPhoneNumber() {
        return testPhoneNumber;
    }

    public void setTestPhoneNumber(String testPhoneNumber) {
        this.testPhoneNumber = testPhoneNumber;
    }
}
