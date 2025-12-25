// package org.facenet.service.scale.persistence;

// import org.facenet.event.MeasurementEvent;
// import org.junit.jupiter.api.AfterEach;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.Mock;
// import org.mockito.junit.jupiter.MockitoExtension;

// import java.time.ZonedDateTime;
// import java.util.concurrent.ExecutorService;
// import java.util.concurrent.TimeUnit;

// import static org.mockito.ArgumentMatchers.any;
// import static org.mockito.Mockito.*;

// /**
//  * Unit tests for BatchPersistenceService
//  */
// @ExtendWith(MockitoExtension.class)
// class BatchPersistenceServiceTest {

//     @Mock
//     private PersistenceService persistenceService;

//     @Mock
//     private ExecutorService batchPersistenceExecutor;

//     private BatchPersistenceService batchPersistenceService;

//     @BeforeEach
//     void setUp() {
//         batchPersistenceService = new BatchPersistenceService(persistenceService, batchPersistenceExecutor);
//     }

//     @AfterEach
//     void tearDown() {
//         batchPersistenceService.stopBatchProcessing();
//     }

//     @Test
//     void testAddToBatch_Success() {
//         // Arrange
//         MeasurementEvent event = createTestEvent(1L);

//         // Act
//         batchPersistenceService.addToBatch(event);

//         // Assert - no exception thrown
//     }

//     @Test
//     void testStartBatchProcessing_SubmitsTask() {
//         // Act
//         batchPersistenceService.startBatchProcessing();

//         // Assert
//         verify(batchPersistenceExecutor, times(1)).submit(any(Runnable.class));
//     }

//     @Test
//     void testStopBatchProcessing() {
//         // Act
//         batchPersistenceService.stopBatchProcessing();

//         // Assert - no exception thrown
//     }

//     @Test
//     void testAddMultipleEventsToBatch() throws InterruptedException {
//         // Arrange
//         MeasurementEvent event1 = createTestEvent(1L);
//         MeasurementEvent event2 = createTestEvent(2L);
//         MeasurementEvent event3 = createTestEvent(3L);

//         // Act
//         batchPersistenceService.addToBatch(event1);
//         batchPersistenceService.addToBatch(event2);
//         batchPersistenceService.addToBatch(event3);

//         // Wait a bit for queue operations
//         TimeUnit.MILLISECONDS.sleep(100);

//         // Assert - no exception thrown
//     }

//     private MeasurementEvent createTestEvent(Long scaleId) {
//         return MeasurementEvent.builder()
//                 .scaleId(scaleId)
//                 .lastTime(ZonedDateTime.now())
//                 .data1("150.5")
//                 .data2("25.3")
//                 .status("online")
//                 .build();
//     }
// }
