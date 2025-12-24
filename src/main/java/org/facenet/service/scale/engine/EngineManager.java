package org.facenet.service.scale.engine;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.facenet.entity.scale.Scale;
import org.facenet.entity.scale.ScaleConfig;
import org.facenet.event.MeasurementEvent;
import org.facenet.repository.scale.ScaleRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Engine Manager - Điều phối và Khởi chạy (Orchestration)
 * 
 * Theo thiết kế Module 3:
 * - Khi ứng dụng khởi động, quét DB và "đề máy" cho tất cả các cân active
 * - Quản lý vòng đời của tất cả engines
 * - Hỗ trợ hot-reload khi cấu hình thay đổi
 */
@Slf4j
@Service
public class EngineManager {
    
    private final ScaleRepository scaleRepository;
    private final BlockingQueue<MeasurementEvent> queue;
    private final ExecutorService deviceEngineExecutor;
    
    // Map lưu trữ các engine đang chạy: scaleId -> engine
    private final Map<Long, ScaleEngine> runningEngines = new ConcurrentHashMap<>();
    
    public EngineManager(
            ScaleRepository scaleRepository,
            @Qualifier("measurementEventQueue") BlockingQueue<MeasurementEvent> queue,
            @Qualifier("deviceEngineExecutor") ExecutorService deviceEngineExecutor) {
        this.scaleRepository = scaleRepository;
        this.queue = queue;
        this.deviceEngineExecutor = deviceEngineExecutor;
    }
    
    /**
     * Khởi động tất cả engines khi ứng dụng ready
     */
    @PostConstruct
    public void startAllEngines() {
        log.info("[EngineManager] Starting all engines...");
        
        try {
            // Lấy tất cả scales active có config
            List<Scale> activeScales = scaleRepository.findAllActiveWithConfig();
            
            if (activeScales.isEmpty()) {
                log.warn("[EngineManager] No active scales found");
                return;
            }
            
            log.info("[EngineManager] Found {} active scales", activeScales.size());
            
            // Khởi động engine cho từng scale
            for (Scale scale : activeScales) {
                try {
                    startEngine(scale);
                } catch (Exception e) {
                    log.error("[EngineManager] Failed to start engine for scale {}: {}", 
                            scale.getId(), e.getMessage(), e);
                }
            }
            
            log.info("[EngineManager] Successfully started {} engines", runningEngines.size());
            
        } catch (Exception e) {
            log.error("[EngineManager] Error starting engines: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Khởi động engine cho một scale
     */
    public void startEngine(Scale scale) {
        ScaleConfig config = scale.getConfig();
        
        if (config == null) {
            log.warn("[EngineManager] Scale {} has no config, skipping", scale.getId());
            return;
        }
        
        // Kiểm tra engine đã chạy chưa
        if (runningEngines.containsKey(scale.getId())) {
            log.warn("[EngineManager] Engine for scale {} already running", scale.getId());
            return;
        }
        
        try {
            // Tạo engine tương ứng protocol
            ScaleEngine engine = EngineFactory.createEngine(config, queue);
            
            // Submit engine vào thread pool
            deviceEngineExecutor.submit(engine);
            
            // Lưu vào map
            runningEngines.put(scale.getId(), engine);
            
            log.info("[EngineManager] Started engine for scale {} (protocol: {})", 
                    scale.getId(), config.getProtocol());
            
        } catch (Exception e) {
            log.error("[EngineManager] Failed to start engine for scale {}: {}", 
                    scale.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to start engine for scale " + scale.getId(), e);
        }
    }
    
    /**
     * Dừng engine của một scale
     */
    public void stopEngine(Long scaleId) {
        ScaleEngine engine = runningEngines.get(scaleId);
        
        if (engine == null) {
            log.warn("[EngineManager] No engine found for scale {}", scaleId);
            return;
        }
        
        try {
            engine.stop();
            runningEngines.remove(scaleId);
            log.info("[EngineManager] Stopped engine for scale {}", scaleId);
        } catch (Exception e) {
            log.error("[EngineManager] Error stopping engine for scale {}: {}", 
                    scaleId, e.getMessage(), e);
        }
    }
    
    /**
     * Restart engine khi cấu hình thay đổi
     */
    public void restartEngine(Long scaleId) {
        log.info("[EngineManager] Restarting engine for scale {}", scaleId);
        
        // Dừng engine cũ
        stopEngine(scaleId);
        
        // Load lại config và start engine mới
        scaleRepository.findByIdWithDetails(scaleId).ifPresent(scale -> {
            if (scale.getIsActive()) {
                startEngine(scale);
            } else {
                log.info("[EngineManager] Scale {} is inactive, not restarting engine", scaleId);
            }
        });
    }
    
    /**
     * Lấy thông tin engine đang chạy
     */
    public Map<Long, ScaleEngine> getRunningEngines() {
        return runningEngines;
    }
    
    /**
     * Kiểm tra engine có đang chạy không
     */
    public boolean isEngineRunning(Long scaleId) {
        ScaleEngine engine = runningEngines.get(scaleId);
        return engine != null && engine.isRunning();
    }
    
    /**
     * Dừng tất cả engines khi ứng dụng shutdown
     */
    @PreDestroy
    public void stopAllEngines() {
        log.info("[EngineManager] Stopping all engines...");
        
        // Dừng tất cả engines
        runningEngines.forEach((scaleId, engine) -> {
            try {
                engine.stop();
                log.info("[EngineManager] Stopped engine for scale {}", scaleId);
            } catch (Exception e) {
                log.error("[EngineManager] Error stopping engine for scale {}: {}", 
                        scaleId, e.getMessage(), e);
            }
        });
        
        runningEngines.clear();
        
        // Shutdown executor
        try {
            deviceEngineExecutor.shutdown();
            if (!deviceEngineExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                log.warn("[EngineManager] Forcing shutdown of device engine executor");
                deviceEngineExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            log.error("[EngineManager] Error during executor shutdown", e);
            deviceEngineExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        log.info("[EngineManager] All engines stopped");
    }
}
