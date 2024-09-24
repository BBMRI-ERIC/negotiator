package eu.bbmri_eric.negotiator.notification.email;

import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.notification.NewNotificationEvent;
import eu.bbmri_eric.negotiator.notification.Notification;
import eu.bbmri_eric.negotiator.notification.NotificationRepository;
import jakarta.transaction.Transactional;
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
    log.info("get here");
    sendOutEmail(notification);
  }

  private void sendOutEmail(Notification notification) {
    Context context = getContext(notification);
    String emailContent = templateEngine.process("negotiation-status-change", context);
    emailService.sendEmail(notification.getRecipient(), "Request Status Update", emailContent);
  }

  private @NonNull Context getContext(Notification notification) {
    Negotiation negotiation = notification.getNegotiation();
    Context context = new Context();
    context.setVariable("recipient", notification.getRecipient());
    context.setVariable("negotiation", negotiation.getId());
    context.setVariable("frontendUrl", frontendUrl);
    context.setVariable("titleForNegotiation", negotiation.getTitle());
    context.setVariable("message", notification.getMessage());
    context.setVariable("emailYoursSincerelyText", emailYoursSincerelyText);
    context.setVariable("emailHelpdeskHref", emailHelpdeskHref);
    return context;
  }
}
