package eu.bbmri_eric.negotiator.notification.internal;

import org.springframework.context.ApplicationEvent;

/** A Notification Event internal */
interface NotificationStrategy<T extends ApplicationEvent> {
  /**
   * Get supported event type
   *
   * @return supported Event type
   */
  Class<T> getSupportedEventType();

  /**
   * Perform necessary notification logic.
   *
   * @param event that was published
   */
  void notify(T event);
}
