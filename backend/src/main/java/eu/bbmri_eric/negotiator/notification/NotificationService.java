package eu.bbmri_eric.negotiator.notification;

import eu.bbmri_eric.negotiator.user.Person;

import java.util.List;
import java.util.Set;

public interface NotificationService {
  Notification createNotification(Long recipientId, String title, String body);

  Notification createNotification(
      Long recipientId, String title, String body, String negotiationId);
  List<Notification> createNotifications(
          Set<Long> recipientIds, String title, String body, String negotiationId);
}
