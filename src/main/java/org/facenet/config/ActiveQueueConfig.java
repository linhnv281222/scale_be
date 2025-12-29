package org.facenet.config;

import org.facenet.event.MeasurementEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Configuration for the Active In-Memory Queue
 * Đây là "Kho chứa đệm" trung tâm - nơi Engine đẩy dữ liệu và Core Worker lấy ra xử lý
 * Theo thiết kế Module 3: Active Queue là BOUNDED, IN-MEMORY, và có WORKER THREADS
 */
@Configuration
public class ActiveQueueConfig {

    private final DeviceEngineProperties properties;

    public ActiveQueueConfig(DeviceEngineProperties properties) {
        this.properties = properties;
    }

    /**
     * Tạo BlockingQueue cho measurement events
     * Capacity: 100k-300k events theo design spec (default: 200k)
     * Nếu đầy, Engine sẽ tự động đợi (backpressure)
     */
    @Bean(name = "measurementEventQueue")
    public BlockingQueue<MeasurementEvent> measurementEventQueue() {
        return new ArrayBlockingQueue<>(properties.getQueueCapacity());
    }

    /**
     * Thread pool cho Core Processing Workers
     * Worker threads: 4-8 theo design spec
     */
    @Bean(name = "coreProcessingExecutor")
    public ExecutorService coreProcessingExecutor() {
        return Executors.newFixedThreadPool(properties.getWorkerThreads());
    }

    /**
     * Thread pool cho Device Engines
     * Mỗi cân chạy trên 1 thread riêng (up to ~300 scales)
     */
    @Bean(name = "deviceEngineExecutor")
    public ExecutorService deviceEngineExecutor() {
        int maxThreads = Math.max(1, properties.getMaxEngineThreads());
        long keepAliveSeconds = Math.max(1, properties.getEngineThreadKeepAliveSeconds());

        ThreadFactory threadFactory = r -> {
            Thread t = new Thread(r);
            t.setName("device-engine-" + t.getId());
            t.setDaemon(true);
            return t;
        };

        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                0,
                maxThreads,
                keepAliveSeconds,
                TimeUnit.SECONDS,
                new SynchronousQueue<>(),
                threadFactory
        );
        executor.allowCoreThreadTimeOut(true);
        return executor;
    }

    /**
     * Thread pool cho Batch Persistence
     * Single thread for batch processing to maintain order
     */
    @Bean(name = "batchPersistenceExecutor")
    public ExecutorService batchPersistenceExecutor() {
        return Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "batch-persistence");
            t.setDaemon(true);
            return t;
        });
    }
}
