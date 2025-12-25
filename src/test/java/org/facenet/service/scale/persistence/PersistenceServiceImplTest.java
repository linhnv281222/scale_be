package org.facenet.service.scale.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.facenet.entity.scale.ScaleCurrentState;
import org.facenet.entity.scale.WeighingLog;
import org.facenet.event.MeasurementEvent;
import org.facenet.repository.scale.ScaleCurrentStateRepository;
import org.facenet.repository.scale.WeighingLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PersistenceServiceImpl
 */
@ExtendWith(MockitoExtension.class)
class PersistenceServiceImplTest {

    @Mock
    private ScaleCurrentStateRepository currentStateRepository;

    @Mock
    private WeighingLogRepository weighingLogRepository;

    @Mock
    private DeadLetterService deadLetterService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private PersistenceServiceImpl persistenceService;

    private MeasurementEvent testEvent;

    @BeforeEach
    void setUp() {
        testEvent = MeasurementEvent.builder()
                .scaleId(1L)
                .lastTime(ZonedDateTime.now())
                .data1("150.5")
                .data2("25.3")
                .data3("98.2")
                .data4("0")
                .data5("1")
                .status("online")
                .build();
    }

    @Test
    void testPersistMeasurement_Success() throws Exception {
        // Arrange
        when(currentStateRepository.findById(1L)).thenReturn(Optional.empty());
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"val\":\"150.5\"}");
        when(currentStateRepository.save(any(ScaleCurrentState.class))).thenAnswer(i -> i.getArgument(0));
        when(weighingLogRepository.save(any(WeighingLog.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        persistenceService.persistMeasurement(testEvent);

        // Assert
        verify(currentStateRepository, times(1)).save(any(ScaleCurrentState.class));
        verify(weighingLogRepository, times(1)).save(any(WeighingLog.class));
        verify(deadLetterService, never()).writeDeadLetter(any(), any());
    }

    @Test
    void testPersistMeasurement_UpdateExistingState() throws Exception {
        // Arrange
        ScaleCurrentState existingState = ScaleCurrentState.builder()
                .scaleId(1L)
                .status("offline")
                .build();
        when(currentStateRepository.findById(1L)).thenReturn(Optional.of(existingState));
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"val\":\"150.5\"}");
        when(currentStateRepository.save(any(ScaleCurrentState.class))).thenAnswer(i -> i.getArgument(0));
        when(weighingLogRepository.save(any(WeighingLog.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        persistenceService.persistMeasurement(testEvent);

        // Assert
        ArgumentCaptor<ScaleCurrentState> stateCaptor = ArgumentCaptor.forClass(ScaleCurrentState.class);
        verify(currentStateRepository).save(stateCaptor.capture());
        
        ScaleCurrentState savedState = stateCaptor.getValue();
        assertEquals("online", savedState.getStatus());
        assertEquals("engine_modbus", savedState.getCreatedBy());
        assertEquals("engine_modbus", savedState.getUpdatedBy());
    }

    @Test
    void testPersistMeasurement_FailureWritesDeadLetter() {
        // Arrange
        when(currentStateRepository.findById(1L)).thenThrow(new RuntimeException("Database connection failed"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> persistenceService.persistMeasurement(testEvent));
        verify(deadLetterService, times(1)).writeDeadLetter(eq(testEvent), any(RuntimeException.class));
    }

    @Test
    void testPersistMeasurement_InsertsWeighingLog() throws Exception {
        // Arrange
        when(currentStateRepository.findById(1L)).thenReturn(Optional.empty());
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"val\":\"150.5\"}");
        when(currentStateRepository.save(any(ScaleCurrentState.class))).thenAnswer(i -> i.getArgument(0));
        when(weighingLogRepository.save(any(WeighingLog.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        persistenceService.persistMeasurement(testEvent);

        // Assert
        ArgumentCaptor<WeighingLog> logCaptor = ArgumentCaptor.forClass(WeighingLog.class);
        verify(weighingLogRepository).save(logCaptor.capture());
        
        WeighingLog savedLog = logCaptor.getValue();
        assertEquals(1L, savedLog.getScaleId());
        assertEquals("engine_modbus", savedLog.getCreatedBy());
        assertEquals("engine_modbus", savedLog.getUpdatedBy());
    }

    @Test
    void testPersistMeasurement_HandlesNullDataFields() throws Exception {
        // Arrange
        testEvent.setData1(null);
        testEvent.setData2(null);
        when(currentStateRepository.findById(1L)).thenReturn(Optional.empty());
        when(currentStateRepository.save(any(ScaleCurrentState.class))).thenAnswer(i -> i.getArgument(0));
        when(weighingLogRepository.save(any(WeighingLog.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        persistenceService.persistMeasurement(testEvent);

        // Assert
        ArgumentCaptor<ScaleCurrentState> stateCaptor = ArgumentCaptor.forClass(ScaleCurrentState.class);
        verify(currentStateRepository).save(stateCaptor.capture());
        
        ScaleCurrentState savedState = stateCaptor.getValue();
        assertNull(savedState.getData1());
        assertNull(savedState.getData2());
    }
}
