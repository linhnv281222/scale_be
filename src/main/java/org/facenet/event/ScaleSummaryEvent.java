package org.facenet.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

/**
 * Event chứa thông tin tổng quan về số lượng cân trong hệ thống
 * Broadcast qua WebSocket để frontend biết có bao nhiêu cân
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScaleSummaryEvent {
    
    /**
     * Tổng số cân trong hệ thống
     */
    private Integer totalScales;
    
    /**
     * Số cân đang active
     */
    private Integer activeScales;
    
    /**
     * Số cân đang offline
     */
    private Integer offlineScales;
    
    /**
     * Số cân đang online (đang đọc được dữ liệu)
     */
    private Integer onlineScales;
    
    /**
     * Thời điểm tạo event
     */
    private ZonedDateTime timestamp;
}
