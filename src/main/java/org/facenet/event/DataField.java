package org.facenet.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data field với name và value
 * Sử dụng cho data1-5 trong MeasurementEvent
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataField {
    
    /**
     * Tên của data field (ví dụ: "Weight", "Temperature", "Humidity")
     */
    private String name;
    
    /**
     * Giá trị của data field (dạng String để giữ nguyên format gốc)
     */
    private String value;
}
