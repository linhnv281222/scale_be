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
     * 5 trường dữ liệu đọc được từ thiết bị
     * Mỗi trường là object chứa name và value
     */
    private DataField data1;
    private DataField data2;
    private DataField data3;
    private DataField data4;
    private DataField data5;

    /**
     * Trạng thái thiết bị tại thời điểm đọc (online, offline, error, etc.)
     */
    private String status;
}
