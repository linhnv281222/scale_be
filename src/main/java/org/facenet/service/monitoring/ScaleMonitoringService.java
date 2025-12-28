package org.facenet.service.monitoring;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.facenet.entity.scale.Scale;
import org.facenet.entity.scale.ScaleCurrentState;
import org.facenet.event.AllScalesDataEvent;
import org.facenet.event.DataField;
import org.facenet.event.ScaleSummaryEvent;
import org.facenet.repository.scale.ScaleCurrentStateRepository;
import org.facenet.repository.scale.ScaleRepository;
import org.facenet.service.scale.engine.EngineManager;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service để broadcast tổng quan số lượng cân
 * Chạy định kỳ để cập nhật frontend về trạng thái tổng thể
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScaleMonitoringService {

    private final ScaleRepository scaleRepository;
    private final ScaleCurrentStateRepository currentStateRepository;
    private final EngineManager engineManager;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Broadcast scale summary mỗi 5 giây
     */
    @Scheduled(fixedRate = 5000)
    public void broadcastScaleSummary() {
        try {
            ScaleSummaryEvent summary = buildScaleSummary();
            
            // Broadcast qua WebSocket
            messagingTemplate.convertAndSend("/topic/scale-summary", summary);
            
            log.debug("[MONITORING] Broadcasted scale summary: total={}, active={}, online={}", 
                    summary.getTotalScales(), summary.getActiveScales(), summary.getOnlineScales());
        } catch (Exception e) {
            log.error("[MONITORING] Error broadcasting scale summary: {}", e.getMessage());
        }
    }

    /**
     * Broadcast toàn bộ dữ liệu của tất cả các cân mỗi 2 giây
     */
    @Scheduled(fixedRate = 180000)
    public void broadcastAllScalesData() {
        try {
            AllScalesDataEvent allData = buildAllScalesData();
            
            // Broadcast qua WebSocket
            messagingTemplate.convertAndSend("/topic/all-scales-data", allData);
            
            log.debug("[MONITORING] Broadcasted all scales data: {} scales", allData.getScales().size());
        } catch (Exception e) {
            log.error("[MONITORING] Error broadcasting all scales data: {}", e.getMessage());
        }
    }

    /**
     * Broadcast scale summary on-demand
     */
    public ScaleSummaryEvent broadcastScaleSummaryNow() {
        ScaleSummaryEvent summary = buildScaleSummary();
        messagingTemplate.convertAndSend("/topic/scale-summary", summary);
        log.info("[MONITORING] Broadcasted scale summary on-demand");
        return summary;
    }

    /**
     * Build scale summary từ database và engine manager
     */
    private ScaleSummaryEvent buildScaleSummary() {
        // Tổng số cân trong DB
        long totalScales = scaleRepository.count();
        
        // Số cân active
        long activeScales = scaleRepository.countByIsActive(true);
        
        // Số cân đang online (có engine đang chạy)
        int onlineScales = 0;
        for (Long scaleId : engineManager.getRunningEngines().keySet()) {
            if (engineManager.isEngineRunning(scaleId)) {
                onlineScales++;
            }
        }
        
        // Số cân offline = active nhưng không có engine
        int offlineScales = (int) (activeScales - onlineScales);
        
        return ScaleSummaryEvent.builder()
                .totalScales((int) totalScales)
                .activeScales((int) activeScales)
                .onlineScales(onlineScales)
                .offlineScales(offlineScales)
                .timestamp(ZonedDateTime.now())
                .build();
    }

    /**
     * Get scale summary without broadcasting
     */
    public ScaleSummaryEvent getScaleSummary() {
        return buildScaleSummary();
    }

    /**
     * Get all scales data without broadcasting
     */
    public AllScalesDataEvent getAllScalesData() {
        return buildAllScalesData();
    }

    /**
     * Build all scales data từ current states
     */
    private AllScalesDataEvent buildAllScalesData() {
        List<AllScalesDataEvent.ScaleData> scaleDataList = new ArrayList<>();
        
        // Lấy tất cả scales active
        List<Scale> activeScales = scaleRepository.findByIsActive(true);
        
        for (Scale scale : activeScales) {
            // Lấy current state
            ScaleCurrentState currentState = currentStateRepository.findById(scale.getId())
                    .orElse(null);
            
            if (currentState != null) {
                AllScalesDataEvent.ScaleData scaleData = AllScalesDataEvent.ScaleData.builder()
                        .scaleId(scale.getId())
                        .scaleName(scale.getName())
                        .status(currentState.getStatus())
                        .lastTime(currentState.getLastTime() != null ? 
                                ZonedDateTime.from(currentState.getLastTime()) : null)
                        .data1(parseDataField(currentState.getData1()))
                        .data2(parseDataField(currentState.getData2()))
                        .data3(parseDataField(currentState.getData3()))
                        .data4(parseDataField(currentState.getData4()))
                        .data5(parseDataField(currentState.getData5()))
                        .build();
                
                scaleDataList.add(scaleData);
            }
        }
        
        return AllScalesDataEvent.builder()
                .scales(scaleDataList)
                .timestamp(ZonedDateTime.now())
                .build();
    }

    /**
     * Parse data field từ String trong DB
     * Tạm thời chỉ có value, không có name (vì DB chỉ lưu value)
     */
    private DataField parseDataField(String value) {
        if (value == null) {
            return null;
        }
        return DataField.builder()
                .value(value)
                .build();
    }
}
