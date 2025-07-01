package eu.bbmri_eric.negotiator.notification.email;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class Scheduler {
    @Scheduled(cron = "${negotiator.email.frequency-cron-expression:0 0 * * * *}")
    void sendOutEmails(){
    }
}
