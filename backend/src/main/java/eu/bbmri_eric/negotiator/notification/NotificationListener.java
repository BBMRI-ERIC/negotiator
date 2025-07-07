package eu.bbmri_eric.negotiator.notification;

import eu.bbmri_eric.negotiator.negotiation.NewNegotiationEvent;
import eu.bbmri_eric.negotiator.post.NewPostEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class NotificationListener {
    List<EventHandler> eventHandlers;

    public NotificationListener(List<EventHandler> eventHandlers) {
        this.eventHandlers = eventHandlers;
    }

    @EventListener(NewNegotiationEvent.class)
    private void onNewNegotiation() {
    }

    ;

    @EventListener(NewPostEvent.class)
    private void onNewPost(NewPostEvent event) {
        eventHandlers.stream().filter(
                handler -> handler.getSupportedEventType().equals(event.getClass())
        ).forEach(handler -> handler.notify(null));
    }

}
