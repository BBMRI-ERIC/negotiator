package eu.bbmri_eric.negotiator.notification;

import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.email.EmailService;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.template.TemplateService;
import eu.bbmri_eric.negotiator.user.Person;
import eu.bbmri_eric.negotiator.user.PersonRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import lombok.NonNull;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/** Listens for requests to send an email notification to a user. */
@Component
@CommonsLog
class EmailNotificationRequestListener {

  private static final DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("d.M.yyyy, HH:mm");

  private final EmailService emailService;
  private final NotificationService notificationService;
  private final PersonRepository personRepository;
  private final NegotiationRepository negotiationRepository;
  private final TemplateService templateService;
  private final String frontendUrl;
  private final String emailYoursSincerelyText;
  private final String emailHelpdeskHref;
  private final String logoURL;

  EmailNotificationRequestListener(
      EmailService emailService,
      NotificationService notificationService,
      PersonRepository personRepository,
      NegotiationRepository negotiationRepository,
      TemplateService templateService,
      @Value("${negotiator.frontend-url}") String frontendUrl,
      @Value("${negotiator.emailYoursSincerelyText}") String emailYoursSincerelyText,
      @Value("${negotiator.emailHelpdeskHref}") String emailHelpdeskHref,
      @Value("${negotiator.emailLogo}") String logoURL) {
    this.emailService = emailService;
    this.notificationService = notificationService;
    this.personRepository = personRepository;
    this.negotiationRepository = negotiationRepository;
    this.templateService = templateService;
    this.frontendUrl = frontendUrl;
    this.emailYoursSincerelyText = emailYoursSincerelyText;
    this.emailHelpdeskHref = emailHelpdeskHref;
    this.logoURL = logoURL;
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
        buildEmailContent(
            emailTemplateName,
            person.getName(),
            notification.getMessage(),
            negotiation != null ? negotiation.getId() : null,
            negotiation != null ? negotiation.getTitle() : null,
            negotiation != null ? negotiation.getCreationDate() : null);

    emailService.sendEmail(person, notification.getTitle(), emailContent);
  }

  @NonNull
  private String buildEmailContent(
      @NonNull String templateName,
      @NonNull String recipientName,
      @NonNull String message,
      String negotiationId,
      String negotiationTitle,
      LocalDateTime negotiationCreationDate) {

    Map<String, Object> variables = new HashMap<>();
    // DO NOT remove any variables without a good reason.
    // Any deployments without updated templates would miss them.
    // If you add or modify them mention it in the documentation.
    variables.put("recipient", recipientName);
    variables.put("message", message);

    if (negotiationId != null && negotiationTitle != null && negotiationCreationDate != null) {
      variables.put("negotiation", negotiationId);
      variables.put("titleForNegotiation", negotiationTitle);
      variables.put("date", negotiationCreationDate.format(DATE_TIME_FORMATTER));
    }
    variables.put("frontendUrl", frontendUrl);
    variables.put("emailYoursSincerelyText", emailYoursSincerelyText);
    variables.put("emailHelpdeskHref", emailHelpdeskHref);
    variables.put("logoUrl", logoURL);

    return templateService.processTemplate(variables, templateName);
  }
}
