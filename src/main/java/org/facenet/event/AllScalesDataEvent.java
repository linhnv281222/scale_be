package org.facenet.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * Event chứa dữ liệu đo được của TẤT CẢ các cân
 * Broadcast qua WebSocket để frontend có snapshot của toàn bộ cân
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AllScalesDataEvent {
    
    /**
     * Danh sách measurement của tất cả các cân
     */
    private List<ScaleData> scales;
    
    /**
     * Thời điểm tạo snapshot
     */
    private ZonedDateTime timestamp;
    
    /**
     * Data của một cân
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScaleData {
        private Long scaleId;
        private String scaleName;
        private String status;
        private ZonedDateTime lastTime;
        private DataField data1;
        private DataField data2;
        private DataField data3;
        private DataField data4;
        private DataField data5;
    }
}
