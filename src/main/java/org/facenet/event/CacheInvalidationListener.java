package org.facenet.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Event listener to handle cache invalidation when configuration or data changes
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CacheInvalidationListener {

    private final CacheManager cacheManager;

    /**
     * Invalidate scale-related caches when scale configuration changes
     */
    @EventListener
    public void handleConfigChanged(ConfigChangedEvent event) {
        log.info("Config changed event received: scaleId={}, changeType={}", 
                event.getScaleId(), event.getChangeType());

        switch (event.getChangeType()) {
            case "CONFIG_UPDATE":
                // Invalidate scale config cache
                invalidateCache("scaleConfig", event.getScaleId());
                log.info("Invalidated cache for scale config: {}", event.getScaleId());
                break;

            case "SCALE_CREATE":
            case "SCALE_UPDATE":
            case "SCALE_DELETE":
                // Invalidate all scale-related caches
                invalidateCacheAll("scales");
                invalidateCacheAll("scalesByLocation");
                invalidateCacheAll("scaleConfig");
                log.info("Invalidated all scale caches due to: {}", event.getChangeType());
                break;
        }
    }

    /**
     * Invalidate location-related caches when location changes
     */
    @EventListener
    public void handleLocationChanged(LocationChangedEvent event) {
        log.info("Location changed event received: locationId={}, changeType={}", 
                event.getLocationId(), event.getChangeType());

        // Invalidate all location and scale caches as scales depend on locations
        invalidateCacheAll("locations");
        invalidateCacheAll("locationsTree");
        invalidateCacheAll("scalesByLocation");
        
        log.info("Invalidated location and related caches");
    }

    private void invalidateCache(String cacheName, Object key) {
        var cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(key);
        }
    }

    private void invalidateCacheAll(String cacheName) {
        var cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }
}
