package org.facenet.service.scale.core;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.facenet.config.DeviceEngineProperties;
import org.facenet.event.MeasurementEvent;
import org.facenet.service.scale.persistence.BatchPersistenceService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Core Processor - "Bộ não" xử lý dữ liệu
 * 
 * Theo thiết kế Module 3:
 * - Core là "kẻ tham ăn" dữ liệu - thấy Queue có hàng là lấy ra ngay
 * - Chạy trên các worker threads riêng biệt (4-8 threads)
 * - KHÔNG block ứng dụng chính
 * 
 * Version 1 (V1): Chỉ ghi LOG để kiểm tra luồng dữ liệu
 * Version 2 (V2): Xử lý nghiệp vụ + Push WebSocket realtime
 * Version 3 (V3): Persistence - Lưu dữ liệu vào DB
 */
@Slf4j
@Component
public class CoreProcessor {
    
    private final BlockingQueue<MeasurementEvent> activeQueue;
    private final ExecutorService coreProcessingExecutor;
    private final SimpMessagingTemplate messagingTemplate;
    private final BatchPersistenceService batchPersistenceService;
    private final DeviceEngineProperties deviceEngineProperties;
    private volatile boolean running = false;
    
    public CoreProcessor(
            @Qualifier("measurementEventQueue") BlockingQueue<MeasurementEvent> activeQueue,
            @Qualifier("coreProcessingExecutor") ExecutorService coreProcessingExecutor,
            SimpMessagingTemplate messagingTemplate,
            BatchPersistenceService batchPersistenceService,
            DeviceEngineProperties deviceEngineProperties) {
        this.activeQueue = activeQueue;
        this.coreProcessingExecutor = coreProcessingExecutor;
        this.messagingTemplate = messagingTemplate;
        this.batchPersistenceService = batchPersistenceService;
        this.deviceEngineProperties = deviceEngineProperties;
    }
    
    /**
     * Khởi động Core Processor khi ứng dụng ready
     */
    @PostConstruct
    public void startProcessing() {
        running = true;
        log.info("[CORE] Starting Core Processor...");
        
        // Start batch persistence processing
        batchPersistenceService.startBatchProcessing();
        
        // Align worker threads with configured pool size
        int numWorkers = Math.max(1, deviceEngineProperties.getWorkerThreads());
        
        for (int i = 0; i < numWorkers; i++) {
            final int workerId = i + 1;
            coreProcessingExecutor.submit(() -> processEvents(workerId));
        }
        
        log.info("[CORE] Started {} worker threads", numWorkers);
    }
    
    /**
     * Worker thread - chạy liên tục để xử lý events từ queue
     */
    private void processEvents(int workerId) {
        log.info("[CORE-Worker-{}] Started", workerId);
        
        while (running) {
            try {
                // Lấy dữ liệu từ Queue (BLOCKING - sẽ đợi nếu queue trống)
                MeasurementEvent event = activeQueue.take();
                
                // V1: Ghi LOG ra console để kiểm tra
                logMeasurementEvent(workerId, event);
                
                // V2: BROADCAST qua WebSocket
                broadcastMeasurement(event);
                
                // V3: PERSISTENCE - Lưu vào DB
                batchPersistenceService.addToBatch(event);
                
            } catch (InterruptedException e) {
                log.warn("[CORE-Worker-{}] Interrupted, stopping...", workerId);
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("[CORE-Worker-{}] Error processing event: {}", workerId, e.getMessage(), e);
                // Không throw exception để worker không bị chết
            }
        }
        
        log.info("[CORE-Worker-{}] Stopped", workerId);
    }
    
    /**
     * V2: Broadcast measurement qua WebSocket
     * 
     * Đẩy dữ liệu tới 2 loại topic:
     * 1. /topic/scales - Topic toàn cục cho tất cả các cân (màn hình tổng quát)
     * 2. /topic/scale/{scaleId} - Topic riêng lẻ cho từng cân (màn hình chi tiết)
     */
    private void broadcastMeasurement(MeasurementEvent event) {
        try {
            // Topic toàn cục: Tất cả các cân đẩy chung về đây
            messagingTemplate.convertAndSend("/topic/scales", event);
            
            // Topic riêng lẻ: Chỉ đẩy dữ liệu của 1 cân cụ thể
            messagingTemplate.convertAndSend("/topic/scale/" + event.getScaleId(), event);
            
            log.debug("[CORE] Broadcasted scale {} data to WebSocket", event.getScaleId());
        } catch (Exception e) {
            log.error("[CORE] Error broadcasting measurement for scale {}: {}", 
                    event.getScaleId(), e.getMessage());
            // Không throw để không ảnh hưởng luồng xử lý chính
        }
    }
    
    /**
     * V1: Log measurement event để kiểm tra luồng dữ liệu
     */
    private void logMeasurementEvent(int workerId, MeasurementEvent event) {
        if (!log.isDebugEnabled()) {
            return;
        }

        log.debug("=====================================");
        log.debug("[CORE-Worker-{}] Received measurement from Scale ID: {}", workerId, event.getScaleId());
        log.debug("[CORE-Worker-{}] Last Time: {}", workerId, event.getLastTime());
        log.debug("[CORE-Worker-{}] Status: {}", workerId, event.getStatus());
        
        // Log với DataField object
        if (event.getData1() != null) {
            String name = event.getData1().getName() != null ? event.getData1().getName() : "Data 1";
            log.debug("[CORE-Worker-{}] {}: {}", workerId, name, event.getData1().getValue());
        }
        
        if (event.getData2() != null) {
            String name = event.getData2().getName() != null ? event.getData2().getName() : "Data 2";
            log.debug("[CORE-Worker-{}] {}: {}", workerId, name, event.getData2().getValue());
        }
        
        if (event.getData3() != null) {
            String name = event.getData3().getName() != null ? event.getData3().getName() : "Data 3";
            log.debug("[CORE-Worker-{}] {}: {}", workerId, name, event.getData3().getValue());
        }
        
        if (event.getData4() != null) {
            String name = event.getData4().getName() != null ? event.getData4().getName() : "Data 4";
            log.debug("[CORE-Worker-{}] {}: {}", workerId, name, event.getData4().getValue());
        }
        
        if (event.getData5() != null) {
            String name = event.getData5().getName() != null ? event.getData5().getName() : "Data 5";
            log.debug("[CORE-Worker-{}] {}: {}", workerId, name, event.getData5().getValue());
        }
        
        log.debug("=====================================");
    }
    
    /**
     * Dừng Core Processor khi ứng dụng shutdown
     */
    @PreDestroy
    public void stopProcessing() {
        log.info("[CORE] Stopping Core Processor...");
        running = false;
        
        // Stop batch persistence
        batchPersistenceService.stopBatchProcessing();
        
        try {
            coreProcessingExecutor.shutdown();
            if (!coreProcessingExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                log.warn("[CORE] Forcing shutdown of core processing executor");
                coreProcessingExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            log.error("[CORE] Error during shutdown", e);
            coreProcessingExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        log.info("[CORE] Core Processor stopped");
    }
}
