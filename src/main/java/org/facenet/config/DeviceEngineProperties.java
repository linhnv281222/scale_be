package org.facenet.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for device engines
 */
@Configuration
@ConfigurationProperties(prefix = "device.engine")
@Data
public class DeviceEngineProperties {

    /**
     * Number of worker threads for processing device data
     */
    private int workerThreads = 8;

    /**
     * Maximum number of threads for device engines.
     * Prevents unbounded thread growth with cached thread pools.
     */
    private int maxEngineThreads = 400;

    /**
     * Engine thread keep-alive time (seconds) when idle.
     */
    private int engineThreadKeepAliveSeconds = 60;

    /**
     * Capacity of the in-memory active queue
     */
    private int queueCapacity = 100000;

    /**
     * Default polling interval in milliseconds
     */
    private int defaultPollInterval = 1000;

    /**
     * Connection timeout in milliseconds
     */
    private int connectionTimeout = 5000;

    /**
     * Read timeout in milliseconds
     */
    private int readTimeout = 3000;
}
