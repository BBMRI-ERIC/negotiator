package eu.bbmri_eric.negotiator.notification;

import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.email.EmailService;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.settings.AdminSettingsRepository;
import eu.bbmri_eric.negotiator.user.Person;
import eu.bbmri_eric.negotiator.user.PersonRepository;
import java.util.List;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/** Listens for requests to send an email notification to a user. */
@Component
@CommonsLog
class EmailNotificationRequestListener {
  private final EmailService emailService;
  private final NotificationService notificationService;
  private final PersonRepository personRepository;
  private final NegotiationRepository negotiationRepository;
  private final EmailContextBuilder emailContextBuilder;
  private final AdminSettingsRepository adminSettingsRepository;

  EmailNotificationRequestListener(
      EmailService emailService,
      NotificationService notificationService,
      PersonRepository personRepository,
      NegotiationRepository negotiationRepository,
      EmailContextBuilder emailContextBuilder,
      AdminSettingsRepository adminSettingsRepository) {
    this.emailService = emailService;
    this.notificationService = notificationService;
    this.personRepository = personRepository;
    this.negotiationRepository = negotiationRepository;
    this.emailContextBuilder = emailContextBuilder;
    this.adminSettingsRepository = adminSettingsRepository;
  }

  @TransactionalEventListener
  @Async
  void onNewNotification(NewNotificationEvent event) {
    NotificationDTO notification = notificationService.findById(event.getNotificationId());
    if (notification == null) {
      log.error("Failed to send email for notification %s".formatted(event.getNotificationId()));
      return;
    }
    sendOutEmail(notification, event.getEmailTemplateName());
  }

  private void sendOutEmail(NotificationDTO notification, String emailTemplateName) {
    Person person =
        personRepository
            .findById(notification.getRecipientId())
            .orElseThrow(() -> new EntityNotFoundException(notification.getRecipientId()));
    List<Person> administrators = personRepository.findAllByAdminIsTrue();
    Negotiation negotiation =
        negotiationRepository.findById(notification.getNegotiationId()).orElse(null);

    String emailContent =
        emailContextBuilder.buildEmailContent(
            person.getName(),
            notification.getMessage(),
            negotiation != null ? negotiation.getId() : null,
            negotiation != null ? negotiation.getTitle() : null,
            negotiation != null ? negotiation.getCreationDate() : null);

    emailService.sendEmail(person, notification.getTitle(), emailContent);
    // Notify also admins for every change in the Negotiation (request status update)
    for (Person admin : administrators) {
      if (notification.getTitle().equals("Request Status update")
          && adminSettingsRepository.getSendNegotiationUpdatesNotifications()) {
        String emailTitle =
            "Admin notification update for negotiation: "
                + (negotiation != null ? negotiation.getId() : "");
        String emailText =
            "There are updates on negotiation "
                + (negotiation != null ? negotiation.getId() : "")
                + ".\n"
                + "Please log into the Negotiator for more details.";
        String adminNotificationEmailContent =
            emailContextBuilder.buildEmailContent(
                admin.getName(),
                emailText,
                negotiation != null ? negotiation.getId() : null,
                negotiation != null ? negotiation.getTitle() : null,
                negotiation != null ? negotiation.getCreationDate() : null);
        emailService.sendEmail(admin, emailTitle, adminNotificationEmailContent);
      }
    }
  }
}
