package eu.bbmri_eric.negotiator.notification;

import org.springframework.context.ApplicationEvent;
import org.springframework.stereotype.Component;

@Component
public interface EventHandler {
    Class<?> getSupportedEventType();
    void notify(ApplicationEvent event);
}
