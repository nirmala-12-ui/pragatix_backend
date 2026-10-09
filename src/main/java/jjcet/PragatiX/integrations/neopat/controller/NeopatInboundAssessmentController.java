package jjcet.PragatiX.integrations.neopat.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jjcet.PragatiX.integrations.neopat.dto.NeopatAssessmentPayloadDto;
import jjcet.PragatiX.integrations.neopat.service.NeopatAssessmentProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/integrations/neopat")
@Tag(name = "NeopatInboundAssessmentController", description = "Inbound Webhook API for Neopat Assessments")
public class NeopatInboundAssessmentController {

    private static final Logger log = LoggerFactory.getLogger(NeopatInboundAssessmentController.class);

    private final NeopatAssessmentProcessingService processingService;
    private final ObjectMapper objectMapper;

    public NeopatInboundAssessmentController(NeopatAssessmentProcessingService processingService,
                                             ObjectMapper objectMapper) {
        this.processingService = processingService;
        this.objectMapper = objectMapper;
    }

    @PostMapping(value = "/assessment", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Receive new assessment payload from Neopat platform")
    public ResponseEntity<Map<String, Boolean>> receiveAssessment(@RequestBody String rawPayload) {
        String extractedEmail = null;
        try {
            if (rawPayload == null || rawPayload.trim().isEmpty()) {
                processingService.recordRawPayloadFailure("", "EMPTY_PAYLOAD", "Payload was empty", null);
                return ResponseEntity.badRequest().body(Map.of("success", false));
            }

            // 1. Pre-parse check & extract email if possible
            try {
                JsonNode root = objectMapper.readTree(rawPayload);
                if (root != null && root.has("email")) {
                    extractedEmail = root.get("email").asText();
                }
            } catch (Exception ex) {
                log.warn("Malformed JSON received from Neopat: {}", ex.getMessage());
                processingService.recordRawPayloadFailure(rawPayload, "MALFORMED_JSON", ex.getMessage(), null);
                return ResponseEntity.badRequest().body(Map.of("success", false));
            }

            // 2. Map to DTO
            NeopatAssessmentPayloadDto payload = objectMapper.readValue(rawPayload, NeopatAssessmentPayloadDto.class);

            // 3. Validate required fields
            if (payload.getEmail() == null || payload.getEmail().isBlank()
                    || payload.getTestId() == null || payload.getTestId().isBlank()
                    || payload.getMarks() == null || payload.getTotalMarks() == null) {
                processingService.recordRawPayloadFailure(
                        rawPayload,
                        "VALIDATION_FAILED",
                        "Missing mandatory fields (email, test_id, marks, or total_marks)",
                        payload.getEmail()
                );
                return ResponseEntity.badRequest().body(Map.of("success", false));
            }

            // 4. Durably persist to current_assessment with complete raw payload
            processingService.receiveAndStoreAssessment(payload, rawPayload);

            // 5. Return strictly {"success": true}
            return ResponseEntity.ok(Map.of("success", true));

        } catch (Exception e) {
            log.error("Error processing Neopat assessment payload: {}", e.getMessage(), e);
            processingService.recordRawPayloadFailure(
                    rawPayload,
                    "INTERNAL_ERROR",
                    e.getMessage(),
                    extractedEmail
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("success", false));
        }
    }
}
