package org.facenet.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Event object that flows through the Active In-Memory Queue
 * Represents a measurement reading from a scale device
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeasurementEvent {

    /**
     * Scale ID that produced this measurement
     */
    private Long scaleId;

    /**
     * Time when device returned the data (not when event was created)
     */
    private OffsetDateTime lastTime;

    /**
     * Data slot 1 (JSONB format)
     */
    private Map<String, Object> data1;

    /**
     * Data slot 2 (JSONB format)
     */
    private Map<String, Object> data2;

    /**
     * Data slot 3 (JSONB format)
     */
    private Map<String, Object> data3;

    /**
     * Data slot 4 (JSONB format)
     */
    private Map<String, Object> data4;

    /**
     * Data slot 5 (JSONB format)
     */
    private Map<String, Object> data5;

    /**
     * Device status (online, offline, error, etc.)
     */
    private String status;

    /**
     * Event creation timestamp (for tracking latency)
     */
    private OffsetDateTime eventCreatedAt;
}
