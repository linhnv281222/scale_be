package org.facenet.service.scale.engine.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConnectionFailureTrackerTest {

    @Test
    void publishesOfflineOnlyAfterContinuousThresholdAndOnlyOnce() {
        ConnectionFailureTracker tracker = new ConnectionFailureTracker();

        assertFalse(tracker.shouldPublishOffline(1_000L, 60_000L));
        assertFalse(tracker.shouldPublishOffline(60_999L, 60_000L));
        assertTrue(tracker.shouldPublishOffline(61_000L, 60_000L));

        tracker.markOfflinePublished();
        assertFalse(tracker.shouldPublishOffline(120_000L, 60_000L));
    }

    @Test
    void successfulReadResetsFailureWindow() {
        ConnectionFailureTracker tracker = new ConnectionFailureTracker();

        assertFalse(tracker.shouldPublishOffline(1_000L, 60_000L));
        tracker.recordSuccess();
        assertFalse(tracker.shouldPublishOffline(60_999L, 60_000L));
        assertTrue(tracker.shouldPublishOffline(120_999L, 60_000L));
    }
}
