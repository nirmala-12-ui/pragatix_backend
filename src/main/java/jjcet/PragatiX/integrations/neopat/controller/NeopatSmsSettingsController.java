package jjcet.PragatiX.integrations.neopat.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jjcet.PragatiX.common.response.ApiResponse;
import jjcet.PragatiX.integrations.neopat.dto.*;
import jjcet.PragatiX.integrations.neopat.service.NeopatSmsOrchestrationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/integrations/neopat/sms")
@Tag(name = "NeopatSmsSettingsController", description = "Super Admin Neopat SMS Settings and Testing APIs")
@SecurityRequirement(name = "bearerAuth")
public class NeopatSmsSettingsController {

    private final NeopatSmsOrchestrationService orchestrationService;

    public NeopatSmsSettingsController(NeopatSmsOrchestrationService orchestrationService) {
        this.orchestrationService = orchestrationService;
    }

    @GetMapping("/settings")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SUPERADMIN')")
    @Operation(summary = "Get Neopat SMS weekly schedule settings")
    public ResponseEntity<ApiResponse<NeopatSmsSettingsResponseDto>> getSettings() {
        NeopatSmsSettingsResponseDto settings = orchestrationService.getSettings();
        return ResponseEntity.ok(ApiResponse.ok("Neopat SMS settings retrieved successfully", settings));
    }

    @PutMapping("/settings")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SUPERADMIN')")
    @Operation(summary = "Update Neopat SMS weekly schedule settings")
    public ResponseEntity<ApiResponse<NeopatSmsSettingsResponseDto>> updateSettings(
            @Valid @RequestBody NeopatSmsScheduleDto scheduleDto) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String updatedBy = auth != null ? auth.getName() : "SUPER_ADMIN";

        NeopatSmsSettingsResponseDto updated = orchestrationService.updateSettings(scheduleDto, updatedBy);
        return ResponseEntity.ok(ApiResponse.ok("Neopat SMS schedule updated successfully", updated));
    }

    @PostMapping("/test")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SUPERADMIN')")
    @Operation(summary = "Trigger a manual SMS test using Twilio without altering the schedule")
    public ResponseEntity<ApiResponse<NeopatTestSmsResponseDto>> triggerTestSms(
            @RequestBody(required = false) NeopatTestSmsRequestDto request) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String triggeredBy = auth != null ? auth.getName() : "SUPER_ADMIN";

        if (request == null) {
            request = new NeopatTestSmsRequestDto();
        }

        NeopatTestSmsResponseDto testResult = orchestrationService.sendManualTestSms(request, triggeredBy);
        return ResponseEntity.ok(ApiResponse.ok("Test SMS processed", testResult));
    }
}
