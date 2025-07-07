package eu.bbmri_eric.negotiator.notification.internal;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * A generic event listener for any Notification worthy events.
 *
 * @author Radovan Tomasik
 */
@Component
class NotificationListener {
    List<NotificationStrategy<? extends ApplicationEvent>> eventHandlers;

    public NotificationListener(List<NotificationStrategy<? extends ApplicationEvent>> eventHandlers) {
        this.eventHandlers = eventHandlers;
    }

    @EventListener
    private void noNewEvent(ApplicationEvent event) {
        eventHandlers.stream().filter(
                handler -> handler.getSupportedEventType().equals(event.getClass())
        ).forEach(handler -> dispatch(handler, event));
    }

    @SuppressWarnings("unchecked")
    private <T extends ApplicationEvent> void dispatch(NotificationStrategy<T> handler, ApplicationEvent event) {
        handler.notify((T) event);
    }
}
