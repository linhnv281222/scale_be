package org.facenet.service.scale.persistence;

import org.facenet.event.MeasurementEvent;

/**
 * Service for persisting measurement data to database
 * Handles both current state updates and historical logging
 */
public interface PersistenceService {

    /**
     * Persist measurement event to database
     * Updates current state and inserts historical log
     *
     * @param event the measurement event to persist
     */
    void persistMeasurement(MeasurementEvent event);
}