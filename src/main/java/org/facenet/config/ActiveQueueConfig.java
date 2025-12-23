package org.facenet.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Configuration for the Active In-Memory Queue
 * This is the core component for data flow as per architecture design
 */
@Configuration
public class ActiveQueueConfig {

    private final DeviceEngineProperties properties;

    public ActiveQueueConfig(DeviceEngineProperties properties) {
        this.properties = properties;
    }

    /**
     * Create the bounded blocking queue for measurement events
     * Capacity: 100k-300k events as per design spec
     */
    @Bean(name = "measurementEventQueue")
    public BlockingQueue<Object> measurementEventQueue() {
        return new ArrayBlockingQueue<>(properties.getQueueCapacity());
    }

    /**
     * Create thread pool for core processing workers
     * Worker threads: 4-8 as per design spec
     */
    @Bean(name = "coreProcessingExecutor")
    public ExecutorService coreProcessingExecutor() {
        return Executors.newFixedThreadPool(properties.getWorkerThreads());
    }

    /**
     * Create thread pool for device engines
     * One thread per scale (up to 300)
     */
    @Bean(name = "deviceEngineExecutor")
    public ExecutorService deviceEngineExecutor() {
        return Executors.newCachedThreadPool();
    }
}
