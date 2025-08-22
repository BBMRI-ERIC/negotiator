package eu.bbmri_eric.negotiator.email;

import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.notification.NewNotificationEvent;
import eu.bbmri_eric.negotiator.notification.NotificationDTO;
import eu.bbmri_eric.negotiator.notification.NotificationService;
import eu.bbmri_eric.negotiator.user.Person;
import eu.bbmri_eric.negotiator.user.PersonRepository;
import jakarta.transaction.Transactional;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/** Listens for requests to send an email. */
@Component
@CommonsLog
class EmailRequestListener {
  private final EmailService emailService;
  private final NotificationService notificationService;
  private final PersonRepository personRepository;
  private final NegotiationRepository negotiationRepository;
  private final EmailContextBuilder emailContextBuilder;

  EmailRequestListener(
      EmailService emailService,
      NotificationService notificationService,
      PersonRepository personRepository,
      NegotiationRepository negotiationRepository,
      EmailContextBuilder emailContextBuilder) {
    this.emailService = emailService;
    this.notificationService = notificationService;
    this.personRepository = personRepository;
    this.negotiationRepository = negotiationRepository;
    this.emailContextBuilder = emailContextBuilder;
  }

  @EventListener(value = NewNotificationEvent.class)
  @TransactionalEventListener
  @Transactional(Transactional.TxType.REQUIRES_NEW)
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
            emailTemplateName,
            person.getName(),
            notification.getMessage(),
            negotiation != null ? negotiation.getId() : null,
            negotiation != null ? negotiation.getTitle() : null,
            negotiation != null ? negotiation.getCreationDate() : null);

    emailService.sendEmail(person, notification.getTitle(), emailContent);
  }
}
