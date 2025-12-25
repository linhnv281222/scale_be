package org.facenet.controller;

import lombok.RequiredArgsConstructor;
import org.facenet.event.MeasurementEvent;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;

/**
 * Test controller để test WebSocket broadcasting
 */
@RestController
@RequestMapping("/api/v1/test")
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
                .data1(weight)
                .data2("1")
                .data3("2")
                .data4("3")
                .data5("4")
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