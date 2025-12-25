package org.facenet.dto.scale;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.beans.ConstructorProperties;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO for scale current state with config data names
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ScaleCurrentStateDto {

    private Long scaleId;
    private String scaleName;
    private String status;
    private OffsetDateTime lastTime;

    /**
     * Map of data field to value and config name
     * e.g., {
     *   "data_1": { "value": "1234.56", "name": "Weight" },
     *   "data_2": { "value": "23.4", "name": "Temperature" }
     * }
     */
    private Map<String, DataFieldValue> dataValues;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataFieldValue {
        private String value;  // Raw value from current_state
        private String name;   // Data name from scale_config
        private boolean isUsed; // Whether this field is configured as used
    }
}
