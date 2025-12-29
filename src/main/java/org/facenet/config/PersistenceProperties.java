package org.facenet.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for Persistence module
 */
@Data
@Component
@ConfigurationProperties(prefix = "persistence")
public class PersistenceProperties {

    /**
     * Batch processing configuration
     */
    private Batch batch = new Batch();

    /**
     * Dead letter configuration
     */
    private DeadLetter deadLetter = new DeadLetter();

    @Data
    public static class Batch {
        /**
         * Number of events to accumulate before persisting
         * Default: 50
         */
        private int size = 50;

        /**
         * Maximum time to wait before persisting a batch (in milliseconds)
         * Default: 500ms
         */
        private long timeoutMs = 500;

        /**
         * Capacity of the internal batch queue.
         * Bounded to prevent OOM when database is slow/down.
         * Default: 100000
         */
        private int queueCapacity = 100000;
    }

    @Data
    public static class DeadLetter {
        /**
         * Directory to store dead letter files
         * Default: dead-letters
         */
        private String directory = "dead-letters";
    }
}
