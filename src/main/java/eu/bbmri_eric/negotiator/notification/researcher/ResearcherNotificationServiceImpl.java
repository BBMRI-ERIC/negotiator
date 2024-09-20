package eu.bbmri_eric.negotiator.notification.researcher;

import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationEvent;
import eu.bbmri_eric.negotiator.notification.Notification;
import eu.bbmri_eric.negotiator.notification.NotificationRepository;
import eu.bbmri_eric.negotiator.notification.email.EmailService;
import eu.bbmri_eric.negotiator.notification.email.NotificationEmailStatus;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@CommonsLog
public class ResearcherNotificationServiceImpl implements ResearcherNotificationService {
  private final NegotiationRepository negotiationRepository;
  private final NotificationRepository notificationRepository;
  private final TemplateEngine templateEngine;
  private final EmailService emailService;

  @Value("${negotiator.frontend-url}")
  private String frontendUrl;

  public ResearcherNotificationServiceImpl(
      NegotiationRepository negotiationRepository,
      NotificationRepository notificationRepository,
      TemplateEngine templateEngine,
      EmailService emailService) {
    this.negotiationRepository = negotiationRepository;
    this.notificationRepository = notificationRepository;
    this.templateEngine = templateEngine;
    this.emailService = emailService;
  }

  @Override
  public void createConfirmationNotification(String negotiationId) {}

  @Override
  public void statusChangeNotification(String negotiationId, NegotiationEvent action) {
    Negotiation negotiation = negotiationRepository.findById(negotiationId).orElse(null);
    if (negotiation == null) {
      log.error(
          "Error creating confirmation notification. Negotiation %s not found"
              .formatted(negotiationId));
      return;
    }
    Notification notification =
        new Notification(
            negotiation.getCreatedBy(),
            negotiation,
            "Your request was %sd by an Administrator.".formatted(action.getLabel().toLowerCase()),
            NotificationEmailStatus.EMAIL_NOT_SENT);
    notificationRepository.save(notification);
    Context context = new Context();
    context.setVariable("recipient", notification.getRecipient());
    context.setVariable("negotiation", negotiationId);
    context.setVariable("frontendUrl", frontendUrl);
    context.setVariable("titleForNegotiation", negotiation.getTitle());
    context.setVariable("message", notification.getMessage());

    String emailContent = templateEngine.process("negotiation-status-change", context);

    emailService.sendEmail(notification.getRecipient(), "New Notifications", emailContent);
  }
}
