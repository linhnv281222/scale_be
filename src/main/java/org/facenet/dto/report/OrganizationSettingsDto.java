package org.facenet.dto.report;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * DTOs for Organization Settings
 */
public class OrganizationSettingsDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Organization settings response")
    public static class Response {
        @Schema(description = "Organization ID")
        private Long id;

        @Schema(description = "Company name (Vietnamese)")
        private String companyName;

        @Schema(description = "Company name (English)")
        private String companyNameEn;

        @Schema(description = "Company address")
        private String address;

        @Schema(description = "Phone number")
        private String phone;

        @Schema(description = "Email address")
        private String email;

        @Schema(description = "Website URL")
        private String website;

        @Schema(description = "Tax code")
        private String taxCode;

        @Schema(description = "Logo resource path (relative to classpath)")
        private String logoUrl;

        @Schema(description = "Logo as Base64 string for UI rendering")
        private String logoBase64;

        @Schema(description = "Whether logo is available")
        private Boolean hasLogo;

        @Schema(description = "Favicon resource path (relative to classpath)")
        private String faviconUrl;

        @Schema(description = "Favicon as Base64 string for UI rendering")
        private String faviconBase64;

        @Schema(description = "Whether favicon is available")
        private Boolean hasFavicon;

        @Schema(description = "Watermark text for reports")
        private String watermarkText;

        @Schema(description = "Whether organization is active")
        private Boolean isActive;

        @Schema(description = "Whether this is the default organization")
        private Boolean isDefault;

        @Schema(description = "Created timestamp")
        private OffsetDateTime createdAt;

        @Schema(description = "Last updated timestamp")
        private OffsetDateTime updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Create organization settings request")
    public static class CreateRequest {
        @Schema(description = "Company name (Vietnamese)", required = true)
        private String companyName;

        @Schema(description = "Company name (English)")
        private String companyNameEn;

        @Schema(description = "Company address")
        private String address;

        @Schema(description = "Phone number")
        private String phone;

        @Schema(description = "Email address")
        private String email;

        @Schema(description = "Website URL")
        private String website;

        @Schema(description = "Tax code")
        private String taxCode;

        @Schema(description = "Watermark text for reports")
        private String watermarkText;

        @Schema(description = "Set as default organization")
        private Boolean isDefault;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Update organization settings request")
    public static class UpdateRequest {
        @Schema(description = "Company name (Vietnamese)")
        private String companyName;

        @Schema(description = "Company name (English)")
        private String companyNameEn;

        @Schema(description = "Company address")
        private String address;

        @Schema(description = "Phone number")
        private String phone;

        @Schema(description = "Email address")
        private String email;

        @Schema(description = "Website URL")
        private String website;

        @Schema(description = "Tax code")
        private String taxCode;

        @Schema(description = "Watermark text for reports")
        private String watermarkText;

        @Schema(description = "Whether organization is active")
        private Boolean isActive;
    }
}
