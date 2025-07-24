package eu.bbmri_eric.negotiator.notification.internal;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NotificationScheduler {

  private final ApplicationEventPublisher eventPublisher;

  public NotificationScheduler(ApplicationEventPublisher eventPublisher) {
    this.eventPublisher = eventPublisher;
  }

  @Scheduled(cron = "${negotiator.notification.reminder-cron-expression:0 0 6 * * *}")
  void forPendingNegotiations() {
    eventPublisher.publishEvent(new PendingNegotiationReminderEvent(this));
  }
}
