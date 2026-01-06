package org.facenet.dto.report;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.OffsetDateTime;

/**
 * DTOs for managing report templates (focus: WORD)
 */
public class ReportTemplateDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WordTemplateResponse {
        private Long id;
        private String code;
        private String name;
        private String description;
        private String titleTemplate;
        private Boolean isActive;
        private Boolean isDefault;
        private String wordTemplateFilename;
        private Boolean hasWordTemplateFile;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateWordTemplateRequest {
        @NotBlank
        private String code;

        @NotBlank
        private String name;

        private String description;

        private String titleTemplate;

        @Builder.Default
        private Boolean isActive = true;

        @Builder.Default
        private Boolean isDefault = false;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateWordTemplateRequest {
        private String name;
        private String description;
        private String titleTemplate;
        private Boolean isActive;
    }

    // ===== Template Import DTOs =====

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TemplateImportRequest {
        @NotBlank
        private String templateCode;

        @NotBlank
        private String templateName;

        private String description;

        private String titleTemplate;

        private String importNotes;

        @Builder.Default
        private Boolean isActive = true;

        private String templateType; // "Báo cáo ca" or "Báo cáo cân"
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TemplateImportResponse {
        private Long id;
        private Long templateId;
        private String templateCode;
        private String originalFilename;
        private String resourcePath;
        private Long fileSizeBytes;
        private String fileHash;
        private String importStatus;
        private OffsetDateTime importDate;
        private String importNotes;
        private Boolean isActive;
        private String templateType; // "Báo cáo ca" or "Báo cáo cân"
        private String createdBy;
        private OffsetDateTime createdAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TemplateImportListResponse {
        private Long id;
        private String templateCode;
        private String originalFilename;
        private String resourcePath;
        private Long fileSizeBytes;
        private String importStatus;
        private OffsetDateTime importDate;
        private Boolean isActive;
        private String templateType; // "Báo cáo ca" or "Báo cáo cân"
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ImportedTemplateDetailsResponse {
        private Long importId;
        private ReportTemplateDto.WordTemplateResponse template;
        private String originalFilename;
        private String resourcePath;
        private Long fileSizeBytes;
        private String fileHash;
        private String importStatus;
        private OffsetDateTime importDate;
        private String importNotes;
    }
}
