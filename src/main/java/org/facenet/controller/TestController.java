package org.facenet.controller;

import lombok.RequiredArgsConstructor;
import org.facenet.event.DataField;
import org.facenet.event.MeasurementEvent;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;

/**
 * Test controller để test WebSocket broadcasting
 */
// Disabled: test-only endpoints removed per requirement
@RequiredArgsConstructor
public class TestController {

    private final SimpMessagingTemplate messagingTemplate;

    @PostMapping("/broadcast/{scaleId}")
    public String broadcastTestMessage(@PathVariable("scaleId") Long scaleId,
                                     @RequestParam(defaultValue = "150.50") String weight) {

        // Tạo test measurement event
        MeasurementEvent testEvent = MeasurementEvent.builder()
                .scaleId(scaleId)
                .lastTime(ZonedDateTime.now())
                .status("TEST")
                .data1(DataField.builder()
                        .name("Weight")
                        .value(weight)
                        .build())
                .data2(DataField.builder()
                        .name("Temperature")
                        .value("25")
                        .build())
                .data3(DataField.builder()
                        .name("Humidity")
                        .value("60")
                        .build())
                .build();

        // Broadcast qua WebSocket
        messagingTemplate.convertAndSend("/topic/scales", testEvent);
        messagingTemplate.convertAndSend("/topic/scale/" + scaleId, testEvent);

        return String.format("✅ Broadcasted test message for scale %d with weight %s", scaleId, weight);
    }

    @GetMapping("/health")
    public String health() {
        return "✅ Server is running and WebSocket is configured";
    }
}