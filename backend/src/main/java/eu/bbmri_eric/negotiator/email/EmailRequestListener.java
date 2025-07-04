package eu.bbmri_eric.negotiator.email;

import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.notification.NewNotificationEvent;
import eu.bbmri_eric.negotiator.notification.Notification;
import eu.bbmri_eric.negotiator.notification.NotificationRepository;
import eu.bbmri_eric.negotiator.user.Person;
import eu.bbmri_eric.negotiator.user.PersonRepository;
import jakarta.transaction.Transactional;
import java.time.format.DateTimeFormatter;
import lombok.NonNull;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/** Listens for requests to send an email. */
@Component
@CommonsLog
public class EmailRequestListener {
  private final EmailService emailService;
  private final TemplateEngine templateEngine;
  private final NotificationRepository notificationRepository;
  private final PersonRepository personRepository;
  private final NegotiationRepository negotiationRepository;

  @Value("${negotiator.frontend-url}")
  private String frontendUrl;

  @Value("${negotiator.emailYoursSincerelyText}")
  private String emailYoursSincerelyText;

  @Value("${negotiator.emailHelpdeskHref}")
  private String emailHelpdeskHref;

  @Value("${negotiator.emailLogo}")
  private String logoURL;

  public EmailRequestListener(
          EmailService emailService,
          TemplateEngine templateEngine,
          NotificationRepository notificationRepository, PersonRepository personRepository, NegotiationRepository negotiationRepository) {
    this.emailService = emailService;
    this.templateEngine = templateEngine;
    this.notificationRepository = notificationRepository;
      this.personRepository = personRepository;
      this.negotiationRepository = negotiationRepository;
  }

  @EventListener(value = NewNotificationEvent.class)
  @TransactionalEventListener
  @Transactional(Transactional.TxType.REQUIRES_NEW)
  protected void handleNewNotificationEvent(NewNotificationEvent event) {
    Notification notification =
        notificationRepository.findById(event.getNotificationId()).orElse(null);
    if (notification == null) {
      log.error("Failed to send email for notification %s".formatted(event.getNotificationId()));
      return;
    }
    sendOutEmail(notification, event.getEmailTemplateName());
  }

  private void sendOutEmail(Notification notification, String emailTemplateName) {
    Person person = personRepository.findById(notification.getRecipientId()).orElseThrow(() -> new EntityNotFoundException(notification.getRecipientId()));
    Negotiation negotiation = negotiationRepository.findById(notification.getNegotiationId()).orElse(null);
    Context context = getContext(notification, person, negotiation);
    String emailContent = templateEngine.process(emailTemplateName, context);
    emailService.sendEmail(person, notification.getTitle(), emailContent);
    notificationRepository.save(notification);
  }

  private @NonNull Context getContext(Notification notification, Person person, Negotiation negotiation) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d.M.yyyy, HH:mm");
    Context context = new Context();
    context.setVariable("recipient", person);
    if (negotiation != null) {
      context.setVariable("negotiation", negotiation.getId());
      context.setVariable("titleForNegotiation", negotiation.getTitle());
      context.setVariable("date", negotiation.getCreationDate().format(formatter));
    }
    context.setVariable("frontendUrl", frontendUrl);
    context.setVariable("message", notification.getMessage());
    context.setVariable("emailYoursSincerelyText", emailYoursSincerelyText);
    context.setVariable("emailHelpdeskHref", emailHelpdeskHref);
    context.setVariable("logoUrl", logoURL);
    return context;
  }
}
