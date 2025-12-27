package org.facenet.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.facenet.entity.scale.Scale;
import org.facenet.repository.scale.ScaleRepository;
import org.facenet.service.monitoring.ScaleMonitoringService;
import org.facenet.service.scale.engine.EngineManager;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Event listener for hot-reload engine when configuration changes
 * 
 * Nhiệm vụ:
 * - Lắng nghe ConfigChangedEvent
 * - Gọi EngineManager để restart engine với config mới
 * - Đảm bảo không restart ứng dụng
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EngineReloadListener {

    private final EngineManager engineManager;
    private final ScaleRepository scaleRepository;
    private final ScaleMonitoringService monitoringService;

    /**
     * Hot-reload engine khi cấu hình hoặc scale thay đổi
     */
    @EventListener
    public void handleConfigChanged(ConfigChangedEvent event) {
        log.info("[EngineReload] Config changed event received: scaleId={}, changeType={}", 
                event.getScaleId(), event.getChangeType());

        switch (event.getChangeType()) {
            case "CONFIG_UPDATE":
                // Cấu hình thay đổi -> Restart engine với config mới
                log.info("[EngineReload] Reloading engine for scale {} due to config update", event.getScaleId());
                engineManager.restartEngine(event.getScaleId());
                log.info("[EngineReload] Engine for scale {} reloaded successfully", event.getScaleId());
                break;

            case "SCALE_CREATE":
                // Scale mới được tạo -> Start engine nếu scale active
                log.info("[EngineReload] Starting engine for new scale {}", event.getScaleId());
                scaleRepository.findByIdWithDetails(event.getScaleId()).ifPresent(scale -> {
                    if (scale.getIsActive()) {
                        engineManager.startEngine(scale);
                        log.info("[EngineReload] Engine for new scale {} started", event.getScaleId());
                    } else {
                        log.info("[EngineReload] Scale {} is inactive, engine not started", event.getScaleId());
                    }
                });
                break;

            case "SCALE_UPDATE":
                // Scale được update (có thể đổi location, active status) -> Restart
                log.info("[EngineReload] Reloading engine for scale {} due to scale update", event.getScaleId());
                engineManager.restartEngine(event.getScaleId());
                log.info("[EngineReload] Engine for scale {} reloaded after update", event.getScaleId());
                break;

            case "SCALE_DELETE":
                // Scale bị xóa -> Stop engine
                log.info("[EngineReload] Stopping engine for deleted scale {}", event.getScaleId());
                engineManager.stopEngine(event.getScaleId());
                log.info("[EngineReload] Engine for scale {} stopped", event.getScaleId());
                break;

            default:
                log.warn("[EngineReload] Unknown change type: {}", event.getChangeType());
        
        // Broadcast scale summary sau khi có thay đổi
        monitoringService.broadcastScaleSummaryNow();
        }
    }
}
