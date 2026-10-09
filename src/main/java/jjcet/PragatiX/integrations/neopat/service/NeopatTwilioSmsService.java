package jjcet.PragatiX.integrations.neopat.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jjcet.PragatiX.integrations.neopat.config.TwilioConfigProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class NeopatTwilioSmsService {

    private static final Logger log = LoggerFactory.getLogger(NeopatTwilioSmsService.class);

    private final TwilioConfigProperties twilioConfig;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public NeopatTwilioSmsService(TwilioConfigProperties twilioConfig, ObjectMapper objectMapper) {
        this.twilioConfig = twilioConfig;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl("https://api.twilio.com/2010-04-01")
                .build();
    }

    public static class TwilioSendResult {
        private final boolean success;
        private final String messageSid;
        private final String errorMessage;

        public TwilioSendResult(boolean success, String messageSid, String errorMessage) {
            this.success = success;
            this.messageSid = messageSid;
            this.errorMessage = errorMessage;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessageSid() {
            return messageSid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    public TwilioSendResult sendSms(String toPhoneNumber, String messageBody) {
        if (!StringUtils.hasText(twilioConfig.getAccountSid()) ||
            !StringUtils.hasText(twilioConfig.getAuthToken()) ||
            twilioConfig.getAccountSid().startsWith("AC_mock")) {
            log.warn("Twilio credentials not configured or set to mock. Simulating SMS to {}: {}", toPhoneNumber, messageBody);
            return new TwilioSendResult(true, "SM_MOCK_" + System.currentTimeMillis(), "Simulated success (Mock Twilio credentials)");
        }

        try {
            String auth = twilioConfig.getAccountSid() + ":" + twilioConfig.getAuthToken();
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("To", toPhoneNumber);
            formData.add("From", twilioConfig.getFromNumber());
            formData.add("Body", messageBody);

            String responseBody = restClient.post()
                    .uri("/Accounts/{accountSid}/Messages.json", twilioConfig.getAccountSid())
                    .header(HttpHeaders.AUTHORIZATION, "Basic " + encodedAuth)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseBody);
            String sid = root.path("sid").asText();
            String status = root.path("status").asText();

            log.info("Twilio SMS sent to {} - SID: {}, status: {}", toPhoneNumber, sid, status);
            return new TwilioSendResult(true, sid, null);
        } catch (Exception e) {
            log.error("Twilio SMS delivery failed for {}: {}", toPhoneNumber, e.getMessage());
            return new TwilioSendResult(false, null, e.getMessage());
        }
    }
}
