package eu.bbmri_eric.negotiator.notification.internal;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.context.ApplicationEvent;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * A generic event listener for any Notification worthy events.
 *
 * @author Radovan Tomasik
 */
@Component
@CommonsLog
class NotificationListener {
  private final List<NotificationStrategy<? extends ApplicationEvent>> eventHandlers;
  private final Map<
          Class<? extends ApplicationEvent>, List<NotificationStrategy<? extends ApplicationEvent>>>
      handlerCache = new ConcurrentHashMap<>();

  NotificationListener(List<NotificationStrategy<? extends ApplicationEvent>> eventHandlers) {
    this.eventHandlers = eventHandlers;
  }

  @PostConstruct
  private void initializeHandlerCache() {
    eventHandlers.forEach(
        handler -> {
          Class<? extends ApplicationEvent> eventType = handler.getSupportedEventType();
          handlerCache.computeIfAbsent(eventType, k -> new ArrayList<>()).add(handler);
        });
  }

  @Async
  @TransactionalEventListener
  protected void onNewEvent(ApplicationEvent event) {
    List<NotificationStrategy<? extends ApplicationEvent>> handlers =
        handlerCache.get(event.getClass());
    if (handlers != null && !handlers.isEmpty()) {
      log.info(
          "Found "
              + handlers.size()
              + " handler(s) for event: "
              + event.getClass().getSimpleName());
      for (NotificationStrategy<? extends ApplicationEvent> handler : handlers) {
        log.info("Executing handler: " + handler.getClass().getSimpleName());
        dispatch(handler, event);
      }
    } else {
      log.debug("No handlers found for event: " + event.getClass().getSimpleName());
    }
  }

  @SuppressWarnings("unchecked")
  private <T extends ApplicationEvent> void dispatch(
      NotificationStrategy<T> handler, ApplicationEvent event) {
    try {
      handler.notify((T) event);
    } catch (Exception e) {
      log.error(
          "Failed to handle notification event: "
              + event.getClass().getSimpleName()
              + " with handler: "
              + handler.getClass().getSimpleName(),
          e);
    }
  }
}
