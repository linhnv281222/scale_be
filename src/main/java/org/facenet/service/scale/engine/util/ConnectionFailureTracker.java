package org.facenet.service.scale.engine.util;

/** Tracks a continuous read-failure window and suppresses duplicate OFFLINE events. */
public final class ConnectionFailureTracker {

    private long failureStartedAt = -1L;
    private boolean offlinePublished;

    public synchronized void recordSuccess() {
        failureStartedAt = -1L;
        offlinePublished = false;
    }

    public synchronized boolean shouldPublishOffline(long now, long thresholdMs) {
        if (failureStartedAt < 0) {
            failureStartedAt = now;
        }
        return !offlinePublished && now - failureStartedAt >= thresholdMs;
    }

    public synchronized void markOfflinePublished() {
        offlinePublished = true;
    }
}
