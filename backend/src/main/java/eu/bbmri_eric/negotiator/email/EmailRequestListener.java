package eu.bbmri_eric.negotiator.email;

import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.notification.NewNotificationEvent;
import eu.bbmri_eric.negotiator.notification.Notification;
import eu.bbmri_eric.negotiator.notification.NotificationRepository;
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
      NotificationRepository notificationRepository) {
    this.emailService = emailService;
    this.templateEngine = templateEngine;
    this.notificationRepository = notificationRepository;
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
    Context context = getContext(notification);
    String emailContent = templateEngine.process(emailTemplateName, context);
    emailService.sendEmail(notification.getRecipient(), notification.getTitle(), emailContent);
    notification.setEmailStatus(NotificationEmailStatus.EMAIL_SENT);
    notificationRepository.save(notification);
  }

  private @NonNull Context getContext(Notification notification) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d.M.yyyy, HH:mm");
    Negotiation negotiation = notification.getNegotiation();
    Context context = new Context();
    context.setVariable("recipient", notification.getRecipient());
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
