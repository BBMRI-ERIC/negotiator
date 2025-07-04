package eu.bbmri_eric.negotiator.notification;

import eu.bbmri_eric.negotiator.user.Person;

public interface NotificationService {
    Notification createNotification(Person recipient, String title, String body);
    Notification createNotification(Person recipient, String title, String body, String negotiationId);
}
