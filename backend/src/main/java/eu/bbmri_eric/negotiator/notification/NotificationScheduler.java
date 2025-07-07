package eu.bbmri_eric.negotiator.notification;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NotificationScheduler {

    RepresentativeNotificationService representativeNotificationService;

    public NotificationScheduler(RepresentativeNotificationService representativeNotificationService) {
        this.representativeNotificationService = representativeNotificationService;
    }

    @Scheduled(cron = "${negotiator.notification.reminder-cron-expression:0 0 6 * * *}")
    void forPendingNegotiations() {
        representativeNotificationService.notifyAboutPendingNegotiations();
    }
}
