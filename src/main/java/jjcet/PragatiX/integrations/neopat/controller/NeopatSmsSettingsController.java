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

import java.util.List;

@RestController
@RequestMapping("/api/v1/integrations/neopat/sms")
@Tag(name = "NeopatSmsSettingsController", description = "Super Admin Neopat SMS Settings and Testing APIs")
@SecurityRequirement(name = "bearerAuth")
public class NeopatSmsSettingsController {

    private final NeopatSmsOrchestrationService orchestrationService;

    public NeopatSmsSettingsController(NeopatSmsOrchestrationService orchestrationService) {
        this.orchestrationService = orchestrationService;
    }

    @GetMapping("/batch-settings")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SUPERADMIN')")
    @Operation(summary = "Get SMS schedule configurations and metrics for all batches (2027-2030)")
    public ResponseEntity<ApiResponse<List<NeopatBatchSmsConfigDto>>> getAllBatchSettings() {
        List<NeopatBatchSmsConfigDto> settings = orchestrationService.getAllBatchSettings();
        return ResponseEntity.ok(ApiResponse.ok("Neopat batch SMS configurations retrieved successfully", settings));
    }

    @GetMapping("/batch-settings/{batchYear}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SUPERADMIN')")
    @Operation(summary = "Get SMS schedule configuration and metrics for a specific batch year")
    public ResponseEntity<ApiResponse<NeopatBatchSmsConfigDto>> getBatchSettings(@PathVariable Integer batchYear) {
        NeopatBatchSmsConfigDto settings = orchestrationService.getBatchSettings(batchYear);
        return ResponseEntity.ok(ApiResponse.ok("Batch " + batchYear + " SMS configuration retrieved successfully", settings));
    }

    @PutMapping("/batch-settings/{batchYear}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SUPERADMIN')")
    @Operation(summary = "Update SMS schedule configuration for a specific batch year")
    public ResponseEntity<ApiResponse<NeopatBatchSmsConfigDto>> updateBatchSettings(
            @PathVariable Integer batchYear,
            @Valid @RequestBody NeopatSmsScheduleDto scheduleDto) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String updatedBy = auth != null ? auth.getName() : "SUPER_ADMIN";

        NeopatBatchSmsConfigDto updated = orchestrationService.updateBatchSettings(batchYear, scheduleDto, updatedBy);
        return ResponseEntity.ok(ApiResponse.ok("Batch " + batchYear + " SMS schedule updated successfully", updated));
    }

    @PostMapping("/batch-settings/{batchYear}/trigger")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SUPERADMIN')")
    @Operation(summary = "Manually trigger SMS dispatch for a specific batch year")
    public ResponseEntity<ApiResponse<NeopatSmsOrchestrationService.BatchExecutionResult>> triggerManualBatchRun(
            @PathVariable Integer batchYear) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String triggeredBy = auth != null ? auth.getName() : "SUPER_ADMIN";

        NeopatSmsOrchestrationService.BatchExecutionResult result = orchestrationService.triggerManualBatchRun(batchYear, triggeredBy);
        return ResponseEntity.ok(ApiResponse.ok("Manual SMS dispatch triggered for Batch " + batchYear, result));
    }

    // Legacy endpoints preserved for backward compatibility
    @GetMapping("/settings")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SUPERADMIN')")
    @Operation(summary = "Get Neopat SMS weekly schedule settings (legacy)")
    public ResponseEntity<ApiResponse<NeopatSmsSettingsResponseDto>> getSettings() {
        NeopatSmsSettingsResponseDto settings = orchestrationService.getSettings();
        return ResponseEntity.ok(ApiResponse.ok("Neopat SMS settings retrieved successfully", settings));
    }

    @PutMapping("/settings")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SUPERADMIN')")
    @Operation(summary = "Update Neopat SMS weekly schedule settings (legacy)")
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
