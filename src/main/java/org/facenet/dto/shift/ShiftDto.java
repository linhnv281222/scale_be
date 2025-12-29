package org.facenet.dto.shift;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.time.OffsetDateTime;

public class ShiftDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {

        @NotBlank(message = "Shift code is required")
        @Size(max = 50, message = "Code must not exceed 50 characters")
        private String code;

        @Size(max = 100, message = "Name must not exceed 100 characters")
        private String name;

        @JsonProperty("start_time")
        private LocalTime startTime;

        @JsonProperty("end_time")
        private LocalTime endTime;

        @JsonProperty("is_active")
        private Boolean isActive;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class UpdateRequest {

        @Size(max = 100, message = "Name must not exceed 100 characters")
        private String name;

        @JsonProperty("start_time")
        private LocalTime startTime;

        @JsonProperty("end_time")
        private LocalTime endTime;

        @JsonProperty("is_active")
        private Boolean isActive;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Response {

        private Long id;

        private String code;

        private String name;

        @JsonProperty("start_time")
        private LocalTime startTime;

        @JsonProperty("end_time")
        private LocalTime endTime;

        @JsonProperty("is_active")
        private Boolean isActive;

        @JsonProperty("created_at")
        private OffsetDateTime createdAt;

        @JsonProperty("created_by")
        private String createdBy;

        @JsonProperty("updated_at")
        private OffsetDateTime updatedAt;

        @JsonProperty("updated_by")
        private String updatedBy;
    }
}
