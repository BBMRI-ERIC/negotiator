package eu.bbmri_eric.negotiator.notification.admin;

import eu.bbmri_eric.negotiator.notification.Notification;
import eu.bbmri_eric.negotiator.notification.NotificationRepository;
import eu.bbmri_eric.negotiator.notification.NotificationService;
import eu.bbmri_eric.negotiator.user.Person;
import eu.bbmri_eric.negotiator.user.PersonRepository;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
@CommonsLog
class AdminNotificationServiceImpl implements AdminNotificationService {

    private final NotificationService notificationService;
    private final PersonRepository personRepository;

    AdminNotificationServiceImpl(NotificationService notificationService, PersonRepository personRepository) {
        this.notificationService = notificationService;
        this.personRepository = personRepository;
    }


    @Override
    public void notifyAllAdmins(String title, String message) {
        Set<Person> admins = new HashSet<>(personRepository.findAllByAdminIsTrue());
        if (admins.isEmpty()) {
           log.error("There are no admins to notify");
        }
        admins.forEach(admin -> {
            notificationService.createNotification(admin, title, message);
        });
    }

    @Override
    public void notifyAllAdmins(String title, String message, String negotiationId) {

    }
}
