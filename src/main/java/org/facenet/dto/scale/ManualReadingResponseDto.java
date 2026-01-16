package org.facenet.dto.scale;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * DTO for manual reading response
 * Contains results of manual reading operation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManualReadingResponseDto {
    
    /**
     * Timestamp when reading was triggered
     */
    @JsonProperty("triggered_at")
    private OffsetDateTime triggeredAt;
    
    /**
     * Reading type that was performed
     */
    @JsonProperty("reading_type")
    private ManualReadingRequestDto.ReadingType readingType;
    
    /**
     * Shift ID if applicable
     */
    @JsonProperty("shift_id")
    private Long shiftId;
    
    /**
     * Shift code if applicable
     */
    @JsonProperty("shift_code")
    private String shiftCode;
    
    /**
     * Total number of scales attempted
     */
    @JsonProperty("total_scales")
    private Integer totalScales;
    
    /**
     * Number of successful readings
     */
    @JsonProperty("successful_readings")
    private Integer successfulReadings;
    
    /**
     * Number of failed readings
     */
    @JsonProperty("failed_readings")
    private Integer failedReadings;
    
    /**
     * Detailed results per scale
     */
    private List<ScaleReadingResult> results;
    
    /**
     * Result for a single scale reading
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScaleReadingResult {
        @JsonProperty("scale_id")
        private Long scaleId;
        
        @JsonProperty("scale_name")
        private String scaleName;
        
        private Boolean success;
        
        private String message;
        
        /**
         * Current data values read from scale
         */
        @JsonProperty("data_values")
        private DataValues dataValues;
    }
    
    /**
     * Data values read from scale
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataValues {
        private String data1;
        private String data2;
        private String data3;
        private String data4;
        private String data5;
        private String status;
    }
}
