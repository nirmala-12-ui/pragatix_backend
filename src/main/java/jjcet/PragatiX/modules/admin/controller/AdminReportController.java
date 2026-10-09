package jjcet.PragatiX.modules.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jjcet.PragatiX.common.response.ApiResponse;
import jjcet.PragatiX.modules.admin.dto.report.AcademicWeekOptionDto;
import jjcet.PragatiX.modules.admin.dto.report.ActivityPointsReportResponse;
import jjcet.PragatiX.modules.admin.service.ActivityReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/reports")
@Tag(name = "AdminReportController", description = "Reports Module endpoints")
@SecurityRequirement(name = "bearerAuth")
public class AdminReportController {

    private final ActivityReportService activityReportService;

    public AdminReportController(ActivityReportService activityReportService) {
        this.activityReportService = activityReportService;
    }

    @GetMapping("/activity-points")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SUPERADMIN', 'ADMIN', 'ROLE_SUPER_ADMIN', 'ROLE_SUPERADMIN', 'ROLE_ADMIN')")
    @Operation(summary = "Get Activity Points Analysis Report", description = "Read-only analytical report for student activity points/awards based on dynamic frequency and filters.")
    public ResponseEntity<ApiResponse<ActivityPointsReportResponse>> getActivityPointsReport(
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) Long semesterId,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) Long stageId,
            @RequestParam(required = false) Long subgroupId,
            @RequestParam Long activityId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer weekNumber,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        ActivityPointsReportResponse response = activityReportService.generateActivityPointsReport(
                academicYear, semesterId, departmentId, sectionId, stageId, subgroupId,
                activityId, date, month, year, weekNumber, search, page, size);

        return ResponseEntity.ok(ApiResponse.ok("Activity points report generated successfully", response));
    }

    @GetMapping("/activity-points/all")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SUPERADMIN', 'ADMIN', 'ROLE_SUPER_ADMIN', 'ROLE_SUPERADMIN', 'ROLE_ADMIN')")
    @Operation(summary = "Get All Activities Points Analysis Report", description = "Generates reports for all activities in scope.")
    public ResponseEntity<ApiResponse<List<ActivityPointsReportResponse>>> getAllActivitiesReport(
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) Long semesterId,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) Long stageId,
            @RequestParam(required = false) Long subgroupId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer weekNumber,
            @RequestParam(required = false) String search) {

        List<ActivityPointsReportResponse> responses = activityReportService.generateAllActivitiesReport(
                academicYear, semesterId, departmentId, sectionId, stageId, subgroupId,
                date, month, year, weekNumber, search);

        return ResponseEntity.ok(ApiResponse.ok("All activities points report generated successfully", responses));
    }

    @GetMapping("/weeks")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SUPERADMIN', 'ADMIN', 'ROLE_SUPER_ADMIN', 'ROLE_SUPERADMIN', 'ROLE_ADMIN')")
    @Operation(summary = "Get available academic weeks for a month/year", description = "Returns week boundaries matching the Academic Calendar configuration.")
    public ResponseEntity<ApiResponse<List<AcademicWeekOptionDto>>> getWeeks(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String academicYear) {

        List<AcademicWeekOptionDto> weeks = activityReportService.getWeeksForMonth(month, year, academicYear);
        return ResponseEntity.ok(ApiResponse.ok("Weeks retrieved successfully", weeks));
    }
}
