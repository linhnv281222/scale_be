package org.facenet.service.scale.persistence;

import org.facenet.entity.scale.ScaleCurrentState;
import org.facenet.entity.scale.WeighingLog;
import org.facenet.event.MeasurementEvent;
import org.facenet.repository.scale.ScaleCurrentStateRepository;
import org.facenet.repository.scale.WeighingLogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for Module 4: Persistence
 * Tests the full persistence flow from event to database
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PersistenceIntegrationTest {

    @Autowired
    private PersistenceService persistenceService;

    @Autowired
    private ScaleCurrentStateRepository currentStateRepository;

    @Autowired
    private WeighingLogRepository weighingLogRepository;

    @Test
    void testPersistMeasurement_CreatesCurrentState() {
        // Arrange
        MeasurementEvent event = createTestEvent(999L);

        // Act
        persistenceService.persistMeasurement(event);

        // Assert
        Optional<ScaleCurrentState> state = currentStateRepository.findById(999L);
        assertTrue(state.isPresent());
        assertEquals("online", state.get().getStatus());
        assertEquals("engine_modbus", state.get().getCreatedBy());
        assertNotNull(state.get().getLastTime());
    }

    @Test
    void testPersistMeasurement_UpdatesExistingState() {
        // Arrange
        ScaleCurrentState existingState = ScaleCurrentState.builder()
                .scaleId(888L)
                .status("offline")
                .lastTime(ZonedDateTime.now().minusHours(1).toOffsetDateTime())
                .build();
        currentStateRepository.save(existingState);

        MeasurementEvent event = createTestEvent(888L);

        // Act
        persistenceService.persistMeasurement(event);

        // Assert
        Optional<ScaleCurrentState> updatedState = currentStateRepository.findById(888L);
        assertTrue(updatedState.isPresent());
        assertEquals("online", updatedState.get().getStatus());
        assertTrue(updatedState.get().getLastTime().isAfter(existingState.getLastTime()));
    }

    @Test
    void testPersistMeasurement_CreatesWeighingLog() {
        // Arrange
        MeasurementEvent event = createTestEvent(777L);

        // Act
        persistenceService.persistMeasurement(event);

        // Assert
        // Note: This is a simplified check. In real scenario, you'd query by composite key
        long logCount = weighingLogRepository.count();
        assertTrue(logCount > 0);
    }

    private MeasurementEvent createTestEvent(Long scaleId) {
        return MeasurementEvent.builder()
                .scaleId(scaleId)
                .lastTime(ZonedDateTime.now())
                .data1("150.5")
                .data2("25.3")
                .data3("98.2")
                .data4("0")
                .data5("1")
                .status("online")
                .build();
    }
}
