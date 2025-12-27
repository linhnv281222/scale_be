package org.facenet.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event fired when scale configuration changes
 */
@Getter
public class ConfigChangedEvent extends ApplicationEvent {
    
    private final Long scaleId;
    private final String changeType; // "CONFIG_UPDATE", "SCALE_CREATE", "SCALE_UPDATE", "SCALE_DELETE"
    
    public ConfigChangedEvent(Object source, Long scaleId, String changeType) {
        super(source);
        this.scaleId = scaleId;
        this.changeType = changeType;
    }
}
