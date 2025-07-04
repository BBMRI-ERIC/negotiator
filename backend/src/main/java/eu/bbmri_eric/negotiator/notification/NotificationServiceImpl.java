package eu.bbmri_eric.negotiator.notification;

import eu.bbmri_eric.negotiator.user.Person;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class NotificationServiceImpl implements NotificationService {
    private final NotificationRepository notificationRepository;
    private final ApplicationEventPublisher eventPublisher;

    private NotificationServiceImpl(NotificationRepository notificationRepository, ApplicationEventPublisher eventPublisher) {
        this.notificationRepository = notificationRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Notification createNotification(Person recipient, String title, String body) {
        Notification notification =  notificationRepository.save(new Notification(recipient.getId(), title, body));
        eventPublisher.publishEvent(new NewNotificationEvent(this, notification.getId()));
        return notification;
    }

    @Override
    public Notification createNotification(Person recipient, String title, String body, String negotiationId) {
        // TODO: Fix use of Negotiation Id
        Notification notification =  notificationRepository.save(new Notification(recipient.getId(), negotiationId, title, body));
        eventPublisher.publishEvent(new NewNotificationEvent(this, notification.getId()));
        return notification;
    }
}
