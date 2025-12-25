package org.facenet.service.scale.persistence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.facenet.config.PersistenceProperties;
import org.facenet.event.MeasurementEvent;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Dead Letter Service for handling persistence failures
 * Writes failed events to a dead letter file for later retry
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeadLetterService {

    private final PersistenceProperties persistenceProperties;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");

    /**
     * Write failed measurement event to dead letter file
     */
    public void writeDeadLetter(MeasurementEvent event, Exception exception) {
        try {
            // Create dead letter directory if not exists
            Path dirPath = Paths.get(persistenceProperties.getDeadLetter().getDirectory());
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }

            // Create filename with timestamp
            String timestamp = LocalDateTime.now().format(FORMATTER);
            String filename = String.format("dead-letter-scale-%d-%s.json", event.getScaleId(), timestamp);
            Path filePath = dirPath.resolve(filename);

            // Write event as JSON
            try (FileWriter writer = new FileWriter(filePath.toFile())) {
                writer.write("{\n");
                writer.write("  \"scaleId\": " + event.getScaleId() + ",\n");
                writer.write("  \"lastTime\": \"" + event.getLastTime() + "\",\n");
                writer.write("  \"data1\": \"" + escapeJson(event.getData1()) + "\",\n");
                writer.write("  \"data2\": \"" + escapeJson(event.getData2()) + "\",\n");
                writer.write("  \"data3\": \"" + escapeJson(event.getData3()) + "\",\n");
                writer.write("  \"data4\": \"" + escapeJson(event.getData4()) + "\",\n");
                writer.write("  \"data5\": \"" + escapeJson(event.getData5()) + "\",\n");
                writer.write("  \"status\": \"" + escapeJson(event.getStatus()) + "\",\n");
                writer.write("  \"error\": \"" + escapeJson(exception.getMessage()) + "\",\n");
                writer.write("  \"timestamp\": \"" + LocalDateTime.now() + "\"\n");
                writer.write("}\n");
            }

            log.warn("[DEAD-LETTER] Wrote failed event for scale {} to {}", event.getScaleId(), filePath);

        } catch (IOException e) {
            log.error("[DEAD-LETTER] Failed to write dead letter for scale {}: {}", event.getScaleId(), e.getMessage());
        }
    }

    /**
     * Escape JSON string values
     */
    private String escapeJson(String value) {
        if (value == null) return "null";
        return value.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
}