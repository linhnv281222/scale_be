package org.facenet.dto.report;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
}
