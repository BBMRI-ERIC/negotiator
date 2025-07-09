package eu.bbmri_eric.negotiator.notification.internal;

import org.springframework.context.ApplicationEvent;

/** Event published when scheduled notifications for pending negotiations should be sent. */
public class PendingNegotiationReminderEvent extends ApplicationEvent {

  public PendingNegotiationReminderEvent(Object source) {
    super(source);
  }
}
