package eu.bbmri_eric.negotiator.user;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class RepresentativeAddedEvent extends ApplicationEvent {
    private final Long resourceId;
    private final Long representativeId;

    public RepresentativeAddedEvent(Object source, Long resourceId, Long representativeId) {
        super(source);
        this.resourceId = resourceId;
        this.representativeId = representativeId;
    }
}
