package eu.bbmri_eric.negotiator.notification.researcher;

import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationEvent;
import eu.bbmri_eric.negotiator.notification.NewNotificationEvent;
import eu.bbmri_eric.negotiator.notification.Notification;
import eu.bbmri_eric.negotiator.notification.NotificationRepository;
import eu.bbmri_eric.negotiator.notification.email.NotificationEmailStatus;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;
import java.util.Objects;
import lombok.NonNull;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@CommonsLog
public class ResearcherNotificationServiceImpl implements ResearcherNotificationService {
  private final NegotiationRepository negotiationRepository;
  private final NotificationRepository notificationRepository;
  private final ApplicationEventPublisher eventPublisher;
  private static final String CREATE_CONFIRMATION_TEMPLATE = "negotiation-confirmation";
  private static final String STATUS_CHANGE_TEMPLATE = "negotiation-status-change";

  public ResearcherNotificationServiceImpl(
      NegotiationRepository negotiationRepository,
      NotificationRepository notificationRepository,
      ApplicationEventPublisher eventPublisher) {
    this.negotiationRepository = negotiationRepository;
    this.notificationRepository = notificationRepository;
    this.eventPublisher = eventPublisher;
  }

  private static @NonNull String getMessage(NegotiationEvent action, String post) {
    String commonBeginning =
        "The request was %sd by an Administrator ".formatted(action.getLabel().toLowerCase());
    String comment = "";
    if (Objects.nonNull(post)) {
      comment =
          "<div>The Administrator added the following comment:</div><div>%s</div>".formatted(post);
    }
    if (action.equals(NegotiationEvent.DECLINE)) {
      return "<div>%s because it did not meet our criteria.</div>%s<div>If you think it was unjustified please reach out to us using the mail address below</div>"
          .formatted(commonBeginning, comment);
    } else {
      return "<div>%s and the representatives of respective organizations were also notified.</div>%s"
          .formatted(commonBeginning, comment);
    }
  }

  @Override
  public void createConfirmationNotification(String negotiationId) {
    String title = "Request Confirmation";
    String message = "Request %s was successfully submitted".formatted(negotiationId);
    publishNotification(negotiationId, title, message, CREATE_CONFIRMATION_TEMPLATE);
  }

  @Override
  public void statusChangeNotification(String negotiationId, NegotiationEvent action) {
    publishStatusChangeNotification(negotiationId, action, null);
  }

  @Override
  public void statusChangeNotification(String negotiationId, NegotiationEvent action, String post) {
    publishStatusChangeNotification(negotiationId, action, post);
  }

  private void publishStatusChangeNotification(
      String negotiationId, NegotiationEvent action, String post) {
    String title = "Request status update";
    String message = getMessage(action, post);
    publishNotification(negotiationId, title, message, STATUS_CHANGE_TEMPLATE);
  }

  @Transactional
  private void publishNotification(
      String negotiationId, String title, String message, String emailTemplate) {
    Negotiation negotiation = negotiationRepository.findById(negotiationId).orElse(null);
    if (negotiation == null) {
      log.error(
          "Error creating the notification. Negotiation %s not found".formatted(negotiationId));
      return;
    }

    Notification notification =
        new Notification(
            negotiation.getCreatedBy(),
            negotiation,
            title,
            message,
            NotificationEmailStatus.EMAIL_NOT_SENT);

    try {
      notification = notificationRepository.save(notification);
    } catch (PersistenceException e) {
      log.error("Error while saving notification %s".formatted(notification.getMessage()), e);
      return;
    }
    eventPublisher.publishEvent(
        new NewNotificationEvent(this, notification.getId(), emailTemplate));
  }
}
