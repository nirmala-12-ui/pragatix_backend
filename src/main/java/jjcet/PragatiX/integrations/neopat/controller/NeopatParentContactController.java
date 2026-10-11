package jjcet.PragatiX.integrations.neopat.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jjcet.PragatiX.common.response.ApiResponse;
import jjcet.PragatiX.integrations.neopat.dto.ParentContactPageResponseDto;
import jjcet.PragatiX.integrations.neopat.dto.ParentContactRequestDto;
import jjcet.PragatiX.integrations.neopat.dto.ParentContactResponseDto;
import jjcet.PragatiX.integrations.neopat.service.NeopatParentContactService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/integrations/neopat/parent-contacts")
@Tag(name = "NeopatParentContactController", description = "Student and Parent Contact Management with Encrypted Mobile Numbers")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SUPERADMIN', 'ADMIN')")
public class NeopatParentContactController {

    private final NeopatParentContactService parentContactService;

    public NeopatParentContactController(NeopatParentContactService parentContactService) {
        this.parentContactService = parentContactService;
    }

    @GetMapping
    @Operation(summary = "Get paginated list of student-parent contact mappings (max 100 per page)")
    public ResponseEntity<ApiResponse<ParentContactPageResponseDto>> getParentContacts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        ParentContactPageResponseDto response = parentContactService.getPaginatedContacts(page, size);
        return ResponseEntity.ok(ApiResponse.ok("Parent contacts retrieved successfully", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single student-parent contact mapping by ID")
    public ResponseEntity<ApiResponse<ParentContactResponseDto>> getParentContactById(@PathVariable Long id) {
        ParentContactResponseDto response = parentContactService.getContactById(id);
        return ResponseEntity.ok(ApiResponse.ok("Parent contact retrieved successfully", response));
    }

    @PostMapping
    @Operation(summary = "Create a new student-parent contact mapping with encrypted mobile number")
    public ResponseEntity<ApiResponse<ParentContactResponseDto>> createParentContact(
            @Valid @RequestBody ParentContactRequestDto requestDto) {
        ParentContactResponseDto created = parentContactService.createContact(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Parent contact created successfully", created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing student-parent contact mapping")
    public ResponseEntity<ApiResponse<ParentContactResponseDto>> updateParentContact(
            @PathVariable Long id,
            @Valid @RequestBody ParentContactRequestDto requestDto) {
        ParentContactResponseDto updated = parentContactService.updateContact(id, requestDto);
        return ResponseEntity.ok(ApiResponse.ok("Parent contact updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a student-parent contact mapping")
    public ResponseEntity<ApiResponse<Void>> deleteParentContact(@PathVariable Long id) {
        parentContactService.deleteContact(id);
        return ResponseEntity.ok(ApiResponse.ok("Parent contact deleted successfully", null));
    }
}
