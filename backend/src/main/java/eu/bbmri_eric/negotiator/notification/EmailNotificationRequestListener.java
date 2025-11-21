package eu.bbmri_eric.negotiator.notification;

import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.email.EmailService;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.user.Person;
import eu.bbmri_eric.negotiator.user.PersonRepository;
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
  private final NotificationRepository notificationRepository;
  private final EmailContextBuilder emailContextBuilder;

  EmailNotificationRequestListener(
      EmailService emailService,
      NotificationService notificationService,
      PersonRepository personRepository,
      NegotiationRepository negotiationRepository,
      NotificationRepository notificationRepository,
      EmailContextBuilder emailContextBuilder) {
    this.emailService = emailService;
    this.notificationService = notificationService;
    this.personRepository = personRepository;
    this.negotiationRepository = negotiationRepository;
    this.notificationRepository = notificationRepository;
    this.emailContextBuilder = emailContextBuilder;
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

    Negotiation negotiation =
        negotiationRepository.findById(notification.getNegotiationId()).orElse(null);

    String emailContent =
        emailContextBuilder.buildEmailContent(
            person.getName(),
            notification.getMessage(),
            negotiation != null ? negotiation.getId() : null,
            negotiation != null ? negotiation.getTitle() : null,
            negotiation != null ? negotiation.getCreationDate() : null);

    String negotiationId = negotiation != null ? negotiation.getId() : null;
    String subject =
        negotiation != null
            ? notification.getTitle()
                + " - "
                + negotiation.getTitle().substring(0, Math.min(negotiation.getTitle().length(), 30))
            : notification.getTitle();

    emailService.sendEmail(person, subject, emailContent, negotiationId);
  }
}
