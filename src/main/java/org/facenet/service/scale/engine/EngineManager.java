package org.facenet.service.scale.engine;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.facenet.entity.scale.Scale;
import org.facenet.entity.scale.ScaleConfig;
import org.facenet.event.MeasurementEvent;
import org.facenet.repository.scale.ScaleRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
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
    private final EntityManager entityManager;
    
    // Map lưu trữ các engine đang chạy: scaleId -> engine
    private final Map<Long, ScaleEngine> runningEngines = new ConcurrentHashMap<>();
    
    public EngineManager(
            ScaleRepository scaleRepository,
            @Qualifier("measurementEventQueue") BlockingQueue<MeasurementEvent> queue,
            @Qualifier("deviceEngineExecutor") ExecutorService deviceEngineExecutor,
            EntityManager entityManager) {
        this.scaleRepository = scaleRepository;
        this.queue = queue;
        this.deviceEngineExecutor = deviceEngineExecutor;
        this.entityManager = entityManager;
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
            
            List<Scale> rtuScales = activeScales.stream()
                    .filter(scale -> isRtuProtocol(scale.getConfig()))
                    .toList();
            startRtuGroups(rtuScales);

            List<Scale> sabusScales = activeScales.stream()
                    .filter(scale -> isSabusProtocol(scale.getConfig()))
                    .toList();
            startSabusGroups(sabusScales);

            // Protocols that do not share a physical serial bus retain one engine per scale.
            for (Scale scale : activeScales.stream()
                    .filter(item -> !isRtuProtocol(item.getConfig()) && !isSabusProtocol(item.getConfig())).toList()) {
                try {
                    startStandaloneEngine(scale);
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
    public synchronized void startEngine(Scale scale) {
        ScaleConfig config = scale.getConfig();
        
        if (config == null) {
            log.warn("[EngineManager] Scale {} has no config, skipping", scale.getId());
            return;
        }
        
        if (isRtuProtocol(config)) {
            // Rebuild RTU groups so a new scale joins the existing physical bus worker.
            reloadAllRtuEngines();
            return;
        }

        if (isSabusProtocol(config)) {
            // Rebuild SABUS groups so a new station joins the existing physical bus worker.
            reloadAllSabusEngines();
            return;
        }

        startStandaloneEngine(scale);
    }

    private void startStandaloneEngine(Scale scale) {
        ScaleConfig config = scale.getConfig();

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

    private void startRtuGroups(List<Scale> rtuScales) {
        Map<String, List<ScaleConfig>> groups = rtuScales.stream()
                .map(Scale::getConfig)
                .collect(Collectors.groupingBy(
                        ModbusRtuEngine::busKey,
                        LinkedHashMap::new,
                        Collectors.toList()));

        groups.forEach((key, configs) -> {
            try {
                ModbusRtuEngine engine = new ModbusRtuEngine(configs, queue);
                deviceEngineExecutor.submit(engine);
                configs.forEach(config -> runningEngines.put(config.getScaleId(), engine));
                log.info("[EngineManager] Started RTU bus {} for scales {}", key, engine.getScaleIds());
            } catch (Exception e) {
                log.error("[EngineManager] Failed to start RTU bus {}: {}", key, e.getMessage(), e);
            }
        });
    }

    private void startSabusGroups(List<Scale> sabusScales) {
        Map<String, List<ScaleConfig>> groups = sabusScales.stream()
                .map(Scale::getConfig)
                .collect(Collectors.groupingBy(
                        SabusAsciiEngine::busKey,
                        LinkedHashMap::new,
                        Collectors.toList()));

        groups.forEach((key, configs) -> {
            try {
                SabusAsciiEngine engine = new SabusAsciiEngine(configs, queue);
                deviceEngineExecutor.submit(engine);
                configs.forEach(config -> runningEngines.put(config.getScaleId(), engine));
                log.info("[EngineManager] Started SABUS bus {} for scales {}", key, engine.getScaleIds());
            } catch (Exception e) {
                log.error("[EngineManager] Failed to start SABUS bus {}: {}", key, e.getMessage(), e);
            }
        });
    }

    private boolean isRtuProtocol(ScaleConfig config) {
        if (config == null || config.getProtocol() == null) {
            return false;
        }
        String protocol = config.getProtocol().trim().toUpperCase(Locale.ROOT);
        return protocol.equals("MODBUS_RTU") || protocol.equals("MODBUSRTU") || protocol.equals("RTU");
    }

    private boolean isSabusProtocol(ScaleConfig config) {
        if (config == null || config.getProtocol() == null) return false;
        String protocol = config.getProtocol().trim().toUpperCase(Locale.ROOT);
        return protocol.equals("SABUS") || protocol.equals("SABUS_ASCII");
    }
    
    /**
     * Dừng engine của một scale
     */
    public synchronized void stopEngine(Long scaleId) {
        ScaleEngine engine = runningEngines.get(scaleId);
        
        if (engine == null) {
            log.warn("[EngineManager] No engine found for scale {}", scaleId);
            return;
        }
        
        if (engine instanceof ModbusRtuEngine) {
            reloadAllRtuEngines();
        } else if (engine instanceof SabusAsciiEngine) {
            reloadAllSabusEngines();
        } else {
            stopStandaloneEngine(scaleId, engine);
        }
    }

    private void stopStandaloneEngine(Long scaleId, ScaleEngine engine) {
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
    @Transactional
    public synchronized void restartEngine(Long scaleId) {
        log.info("[EngineManager] Restarting engine for scale {}", scaleId);

        ScaleEngine previousEngine = runningEngines.get(scaleId);
        
        // CRITICAL FIX: Clear persistence context để tránh lấy cached entity cũ
        // JPA EntityManager có thể cache entity, dẫn đến việc không load config mới từ DB
        entityManager.clear();
        log.debug("[EngineManager] Cleared EntityManager persistence context before reloading config");
        
        // Load the fresh config before choosing whether the whole RTU bus must be rebuilt.
        scaleRepository.findByIdWithDetails(scaleId).ifPresent(scale -> {
            ScaleConfig config = scale.getConfig();
            if (config != null) {
                log.info("[EngineManager] Loaded fresh config from DB - scaleId={}, protocol={}, pollInterval={}ms",
                        scaleId, config.getProtocol(), config.getPollInterval());
            }
            
            boolean wasRtu = previousEngine instanceof ModbusRtuEngine;
            boolean isRtu = isRtuProtocol(config);
            boolean wasSabus = previousEngine instanceof SabusAsciiEngine;
            boolean isSabus = isSabusProtocol(config);

            if (wasRtu || isRtu) {
                if (wasSabus) {
                    stopAllSabusEngines();
                } else if (!wasRtu && previousEngine != null) {
                    stopStandaloneEngine(scaleId, previousEngine);
                }
                reloadAllRtuEngines();
                if (scale.getIsActive() && !isRtu) {
                    if (isSabus) reloadAllSabusEngines();
                    else startStandaloneEngine(scale);
                }
            } else if (wasSabus || isSabus) {
                if (!wasSabus && previousEngine != null) {
                    stopStandaloneEngine(scaleId, previousEngine);
                }
                reloadAllSabusEngines();
                if (scale.getIsActive() && !isSabus) startStandaloneEngine(scale);
            } else if (scale.getIsActive()) {
                if (previousEngine != null) {
                    stopStandaloneEngine(scaleId, previousEngine);
                }
                startStandaloneEngine(scale);
            } else {
                if (previousEngine != null) {
                    stopStandaloneEngine(scaleId, previousEngine);
                }
                log.info("[EngineManager] Scale {} is inactive, not restarting engine", scaleId);
            }
        });
    }

    private void reloadAllRtuEngines() {
        stopAllRtuEngines();
        entityManager.clear();
        List<Scale> rtuScales = scaleRepository.findAllActiveWithConfig().stream()
                .filter(scale -> isRtuProtocol(scale.getConfig()))
                .toList();
        startRtuGroups(rtuScales);
    }

    private void stopAllRtuEngines() {
        Set<ScaleEngine> engines = runningEngines.values().stream()
                .filter(ModbusRtuEngine.class::isInstance)
                .collect(Collectors.toCollection(HashSet::new));
        engines.forEach(ScaleEngine::stop);
        runningEngines.entrySet().removeIf(entry -> entry.getValue() instanceof ModbusRtuEngine);
    }

    private void reloadAllSabusEngines() {
        stopAllSabusEngines();
        entityManager.clear();
        List<Scale> sabusScales = scaleRepository.findAllActiveWithConfig().stream()
                .filter(scale -> isSabusProtocol(scale.getConfig()))
                .toList();
        startSabusGroups(sabusScales);
    }

    private void stopAllSabusEngines() {
        Set<ScaleEngine> engines = runningEngines.values().stream()
                .filter(SabusAsciiEngine.class::isInstance)
                .collect(Collectors.toCollection(HashSet::new));
        engines.forEach(ScaleEngine::stop);
        runningEngines.entrySet().removeIf(entry -> entry.getValue() instanceof SabusAsciiEngine);
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
        
        // Shared RTU engines appear under multiple scale IDs, so stop each instance once.
        Set<ScaleEngine> uniqueEngines = new HashSet<>(runningEngines.values());
        uniqueEngines.forEach(engine -> {
            try {
                engine.stop();
                log.info("[EngineManager] Stopped engine {}", engine.getClass().getSimpleName());
            } catch (Exception e) {
                log.error("[EngineManager] Error stopping engine: {}", e.getMessage(), e);
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
