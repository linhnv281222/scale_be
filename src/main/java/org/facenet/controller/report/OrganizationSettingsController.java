package org.facenet.controller.report;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.facenet.dto.report.OrganizationSettingsDto;
import org.facenet.service.report.OrganizationSettingsService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Controller for managing organization settings and branding
 */
@RestController
@RequestMapping("/api/organization-settings")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Organization Settings", description = "APIs for managing organization information and branding")
public class OrganizationSettingsController {

    private final OrganizationSettingsService organizationSettingsService;

    @GetMapping
    @Operation(
        summary = "Get organization settings",
        description = "Get organization information including company details and logo (Base64 encoded for UI rendering)",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Success",
                content = @Content(schema = @Schema(implementation = OrganizationSettingsDto.Response.class))
            )
        }
    )
    public ResponseEntity<OrganizationSettingsDto.Response> getSettings() {
        return ResponseEntity.ok(organizationSettingsService.getActiveSettings());
    }

    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    @Operation(
        summary = "Create organization settings",
        description = "Create new organization with company information, optional logo and favicon",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Created successfully",
                content = @Content(schema = @Schema(implementation = OrganizationSettingsDto.Response.class))
            )
        }
    )
    public ResponseEntity<OrganizationSettingsDto.Response> createSettings(
            @RequestParam String companyName,
            @RequestParam(required = false) String companyNameEn,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String website,
            @RequestParam(required = false) String taxCode,
            @RequestParam(required = false) String watermarkText,
            @RequestParam(required = false) MultipartFile logo,
            @RequestParam(required = false) MultipartFile favicon) {
        
        OrganizationSettingsDto.CreateRequest request = OrganizationSettingsDto.CreateRequest.builder()
                .companyName(companyName)
                .companyNameEn(companyNameEn)
                .address(address)
                .phone(phone)
                .email(email)
                .website(website)
                .taxCode(taxCode)
                .watermarkText(watermarkText)
                .build();
        
        return ResponseEntity.ok(organizationSettingsService.createSettings(request, logo, favicon));
    }

    @PutMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @Operation(
        summary = "Update organization settings",
        description = "Update organization information. Can update text fields via form data or upload logo/favicon. All fields are optional.",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Updated successfully",
                content = @Content(schema = @Schema(implementation = OrganizationSettingsDto.Response.class))
            )
        }
    )
    public ResponseEntity<OrganizationSettingsDto.Response> updateSettings(
            @RequestParam(required = false) String companyName,
            @RequestParam(required = false) String companyNameEn,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String website,
            @RequestParam(required = false) String taxCode,
            @RequestParam(required = false) String watermarkText,
            @RequestParam(required = false) MultipartFile logo,
            @RequestParam(required = false) Boolean deleteLogo,
            @RequestParam(required = false) MultipartFile favicon,
            @RequestParam(required = false) Boolean deleteFavicon) {
        
        OrganizationSettingsDto.UpdateRequest request = OrganizationSettingsDto.UpdateRequest.builder()
                .companyName(companyName)
                .companyNameEn(companyNameEn)
                .address(address)
                .phone(phone)
                .email(email)
                .website(website)
                .taxCode(taxCode)
                .watermarkText(watermarkText)
                .build();
        
        return ResponseEntity.ok(organizationSettingsService.updateActiveSettings(request, logo, deleteLogo, favicon, deleteFavicon));
    }
}
