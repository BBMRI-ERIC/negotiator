package eu.bbmri_eric.negotiator.notification;

import jakarta.transaction.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@Transactional
class NotificationServiceImpl implements NotificationService {
    private final NotificationRepository notificationRepository;
    private final ApplicationEventPublisher eventPublisher;

    NotificationServiceImpl(
            NotificationRepository notificationRepository, ApplicationEventPublisher eventPublisher) {
        this.notificationRepository = notificationRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Notification createNotification(Long recipientId, String title, String body) {
        Notification notification =
                notificationRepository.save(new Notification(recipientId, title, body));
        eventPublisher.publishEvent(new NewNotificationEvent(this, notification.getId()));
        return notification;
    }

    @Override
    public Notification createNotification(
            Long recipientId, String title, String body, String negotiationId) {
        Notification notification =
                notificationRepository.save(
                        new Notification(recipientId, negotiationId, title, body));
        eventPublisher.publishEvent(new NewNotificationEvent(this, notification.getId()));
        return notification;
    }

    @Override
    public List<Notification> createNotifications(Set<Long> recipientIds, String title, String body, String negotiationId) {
        List<Notification> notifications = new ArrayList<>();
        for (Long recipientId : recipientIds) {
            Notification notification = new Notification(recipientId, title, body, negotiationId);
            notifications.add(notificationRepository.save(notification));
            eventPublisher.publishEvent(new NewNotificationEvent(this, notification.getId()));
        }
        return notifications;
    }
}
