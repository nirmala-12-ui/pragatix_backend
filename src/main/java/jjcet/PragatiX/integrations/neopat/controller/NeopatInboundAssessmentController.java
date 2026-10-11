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

            log.info("Received Neopat inbound raw payload: {}", rawPayload);

            JsonNode root;
            // 1. Pre-parse check & extract email if possible
            try {
                root = objectMapper.readTree(rawPayload);
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
            if (payload.getTestName() == null || payload.getTestName().isBlank()) {
                String candidate = findTestNameInJson(root);
                if (candidate != null && !candidate.isBlank()) {
                    payload.setTestName(candidate);
                }
            }

            // 3. Validate required fields
            String testIdentifier = payload.getEffectiveTestName();
            if (payload.getEmail() == null || payload.getEmail().isBlank()
                    || testIdentifier == null || testIdentifier.isBlank()
                    || payload.getMarks() == null || payload.getTotalMarks() == null) {
                processingService.recordRawPayloadFailure(
                        rawPayload,
                        "VALIDATION_FAILED",
                        "Missing mandatory fields (email, test_id/test_name, marks, or total_marks)",
                        payload.getEmail()
                );
                return ResponseEntity.badRequest().body(Map.of("success", false));
            }

            // 4. Batch filter, evaluate, and store if eligible
            NeopatAssessmentProcessingService.InboundAssessmentResult result =
                    processingService.processInboundAssessment(payload, rawPayload);

            if (result.skipped()) {
                log.info("Inbound assessment skipped for email '{}': {}", payload.getEmail(), result.reason());
            }

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

    private String findTestNameInJson(JsonNode root) {
        if (root == null) return null;
        String[] candidateFields = {
                "title", "test_name", "testname", "exam_title", "examTitle",
                "exam_name", "examName", "test_title", "testTitle",
                "assessment_name", "assessmentname", "assessment_title", "assessmentTitle",
                "name"
        };
        for (String field : candidateFields) {
            if (root.hasNonNull(field)) {
                return root.get(field).asText();
            }
        }
        String[] nestedObjects = {"exam", "assessment", "test"};
        for (String nested : nestedObjects) {
            if (root.has(nested) && root.get(nested).isObject()) {
                JsonNode obj = root.get(nested);
                for (String field : candidateFields) {
                    if (obj.hasNonNull(field)) {
                        return obj.get(field).asText();
                    }
                }
            }
        }
        return null;
    }
}
