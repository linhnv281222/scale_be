package org.facenet.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

/**
 * Event chứa dữ liệu đo từ cân - "Bao thư" chạy xuyên suốt từ Engine -> Queue -> Core
 * Theo thiết kế Module 3, tất cả dữ liệu thô được giữ dạng String để giữ nguyên những gì Engine "thấy"
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeasurementEvent {

    /**
     * ID của cân (scale_id)
     */
    private Long scaleId;

    /**
     * Thời điểm đọc được từ thiết bị (không phải thời điểm insert DB)
     */
    private ZonedDateTime lastTime;

    /**
     * 5 trường dữ liệu thô đọc được từ thiết bị
     * Luôn để String để giữ nguyên format gốc từ Engine (có thể là "0001", "1.0", v.v.)
     * Việc convert sang Double/Integer sẽ làm ở Core Processing
     */
    private String data1;
    private String data2;
    private String data3;
    private String data4;
    private String data5;

    /**
     * Trạng thái thiết bị tại thời điểm đọc (online, offline, error, etc.)
     */
    private String status;
}
