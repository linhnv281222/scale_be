package org.facenet.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event fired when location changes
 */
@Getter
public class LocationChangedEvent extends ApplicationEvent {
    
    private final Long locationId;
    private final String changeType; // "CREATE", "UPDATE", "DELETE"
    
    public LocationChangedEvent(Object source, Long locationId, String changeType) {
        super(source);
        this.locationId = locationId;
        this.changeType = changeType;
    }
}
