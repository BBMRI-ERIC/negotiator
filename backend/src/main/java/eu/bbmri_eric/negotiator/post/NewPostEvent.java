package eu.bbmri_eric.negotiator.post;

import org.springframework.context.ApplicationEvent;

public class NewPostEvent extends ApplicationEvent {
    public NewPostEvent(Object source) {
        super(source);
    }
}
