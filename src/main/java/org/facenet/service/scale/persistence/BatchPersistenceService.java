package org.facenet.service.scale.persistence;

import lombok.extern.slf4j.Slf4j;
import org.facenet.config.PersistenceProperties;
import org.facenet.event.MeasurementEvent;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Batch Persistence Service
 * Accumulates measurement events and persists them in batches for better performance
 */
@Slf4j
@Service
public class BatchPersistenceService {

    private final PersistenceService persistenceService;
    private final ExecutorService batchPersistenceExecutor;
    private final PersistenceProperties persistenceProperties;
    private final BlockingQueue<MeasurementEvent> batchQueue = new LinkedBlockingQueue<>();
    private volatile boolean running = true;

    public BatchPersistenceService(
            PersistenceService persistenceService,
            @Qualifier("batchPersistenceExecutor") ExecutorService batchPersistenceExecutor,
            PersistenceProperties persistenceProperties) {
        this.persistenceService = persistenceService;
        this.batchPersistenceExecutor = batchPersistenceExecutor;
        this.persistenceProperties = persistenceProperties;
    }

    /**
     * Add measurement event to batch queue
     */
    public void addToBatch(MeasurementEvent event) {
        try {
            batchQueue.put(event);
        } catch (InterruptedException e) {
            log.warn("[BATCH] Interrupted while adding event to batch queue for scale {}", event.getScaleId());
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Start batch processing in background thread
     */
    public void startBatchProcessing() {
        batchPersistenceExecutor.submit(this::processBatches);
        log.info("[BATCH] Started batch persistence processing with batch-size={}, timeout={}ms", 
                persistenceProperties.getBatch().getSize(), 
                persistenceProperties.getBatch().getTimeoutMs());
    }

    /**
     * Process batches continuously
     */
    private void processBatches() {
        log.info("[BATCH] Batch processing thread started");

        while (running) {
            try {
                processBatch();
            } catch (Exception e) {
                log.error("[BATCH] Error in batch processing: {}", e.getMessage(), e);
            }
        }

        log.info("[BATCH] Batch processing thread stopped");
    }

    /**
     * Process one batch of events
     */
    private void processBatch() {
        List<MeasurementEvent> batch = new java.util.ArrayList<>();

        try {
            // Wait for first event
            MeasurementEvent firstEvent = batchQueue.poll(
                    persistenceProperties.getBatch().getTimeoutMs(), 
                    TimeUnit.MILLISECONDS);
            if (firstEvent != null) {
                batch.add(firstEvent);

                // Drain remaining events up to batch size
                batchQueue.drainTo(batch, persistenceProperties.getBatch().getSize() - 1);

                // Persist the batch
                persistBatch(batch);
            }
        } catch (InterruptedException e) {
            log.warn("[BATCH] Batch processing interrupted");
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Persist a batch of events
     */
    private void persistBatch(List<MeasurementEvent> batch) {
        long startTime = System.currentTimeMillis();

        for (MeasurementEvent event : batch) {
            try {
                persistenceService.persistMeasurement(event);
            } catch (Exception e) {
                log.error("[BATCH] Failed to persist event for scale {}: {}", event.getScaleId(), e.getMessage());
                // Continue with other events in batch
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        log.debug("[BATCH] Persisted {} events in {}ms", batch.size(), duration);
    }

    /**
     * Stop batch processing
     */
    public void stopBatchProcessing() {
        running = false;
        log.info("[BATCH] Stopping batch persistence processing");
    }
}