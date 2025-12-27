package org.facenet.service.scale.persistence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.facenet.entity.scale.Scale;
import org.facenet.entity.scale.ScaleCurrentState;
import org.facenet.entity.scale.WeighingLog;
import org.facenet.event.MeasurementEvent;
import org.facenet.repository.scale.ScaleCurrentStateRepository;
import org.facenet.repository.scale.ScaleRepository;
import org.facenet.repository.scale.WeighingLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * Implementation of PersistenceService
 * Handles batching and asynchronous persistence for high throughput
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PersistenceServiceImpl implements PersistenceService {

    private final ScaleCurrentStateRepository currentStateRepository;
    private final WeighingLogRepository weighingLogRepository;
    private final ScaleRepository scaleRepository;
    private final DeadLetterService deadLetterService;

    @Override
    @Transactional
    public void persistMeasurement(MeasurementEvent event) {
        try {
            // Update current state (upsert)
            updateCurrentState(event);

            // Insert historical log
            insertWeighingLog(event);

            log.debug("[PERSISTENCE] Successfully persisted measurement for scale {}", event.getScaleId());
        } catch (Exception e) {
            log.error("[PERSISTENCE] Error persisting measurement for scale {}: {}", event.getScaleId(), e.getMessage(), e);
            // Write to dead letter file
            deadLetterService.writeDeadLetter(event, e);
            // Re-throw to let batch service handle
            throw e;
        }
    }

    /**
     * Update or insert current state for the scale
     */
    private void updateCurrentState(MeasurementEvent event) {
        Scale scale = scaleRepository.findById(event.getScaleId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Scale not found with id: " + event.getScaleId()));

        ScaleCurrentState currentState = currentStateRepository.findById(event.getScaleId())
                .orElseGet(() -> {
                    // For new state, create with scale relationship
                    ScaleCurrentState newState = new ScaleCurrentState();
                    newState.setScale(scale); // This sets the ID via @MapsId
                    return newState;
                });

        // Map data fields - lấy value từ DataField object
        currentState.setData1(event.getData1() != null ? event.getData1().getValue() : null);
        currentState.setData2(event.getData2() != null ? event.getData2().getValue() : null);
        currentState.setData3(event.getData3() != null ? event.getData3().getValue() : null);
        currentState.setData4(event.getData4() != null ? event.getData4().getValue() : null);
        currentState.setData5(event.getData5() != null ? event.getData5().getValue() : null);
        currentState.setStatus(event.getStatus());
        currentState.setLastTime(event.getLastTime().toOffsetDateTime());

        // Set audit fields for system operation
        currentState.setCreatedBy("engine_modbus");
        currentState.setUpdatedBy("engine_modbus");

        currentStateRepository.save(currentState);
    }

    /**
     * Insert new weighing log entry
     */
    private void insertWeighingLog(MeasurementEvent event) {
        WeighingLog log = WeighingLog.builder()
                .scaleId(event.getScaleId())
                .createdAt(OffsetDateTime.now()) // Set createdAt explicitly for composite key
                .lastTime(event.getLastTime().toOffsetDateTime())
                .data1(event.getData1() != null ? event.getData1().getValue() : null)
                .data2(event.getData2() != null ? event.getData2().getValue() : null)
                .data3(event.getData3() != null ? event.getData3().getValue() : null)
                .data4(event.getData4() != null ? event.getData4().getValue() : null)
                .data5(event.getData5() != null ? event.getData5().getValue() : null)
                .createdBy("engine_modbus")
                .updatedBy("engine_modbus")
                .build();

        weighingLogRepository.save(log);
    }
}