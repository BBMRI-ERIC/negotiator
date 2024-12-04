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

  public ResearcherNotificationServiceImpl(
      NegotiationRepository negotiationRepository,
      NotificationRepository notificationRepository,
      ApplicationEventPublisher eventPublisher) {
    this.negotiationRepository = negotiationRepository;
    this.notificationRepository = notificationRepository;
    this.eventPublisher = eventPublisher;
  }

  @Override
  public void createConfirmationNotification(String negotiationId) {
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
            "Request Confirmation",
            "Request %s was successfully submitted".formatted(negotiationId),
            NotificationEmailStatus.EMAIL_NOT_SENT);
    try {
      notification = notificationRepository.save(notification);
    } catch (PersistenceException e) {
      log.error("Error while saving notification %s".formatted(notification.getMessage()), e);
      return;
    }
    eventPublisher.publishEvent(
        new NewNotificationEvent(this, notification.getId(), "negotiation-confirmation"));
  }

  private static @NonNull String getMessage(NegotiationEvent action, String post) {
    if (action.equals(NegotiationEvent.DECLINE)) {
      String motivation = "";
      if (Objects.nonNull(post)) {
        motivation = "The Administrator gave the following motivation: %s".formatted(post);
      }
      return "The request was %sd by an Administrator because it did not meet our criteria. %s. If you think it was unjustified please reach out to us using the mail address below"
          .formatted(action.getLabel().toLowerCase(), motivation);
    } else {
      return "The request was %sd by an Administrator and the representatives of respective organizations were also notified."
          .formatted(action.getLabel().toLowerCase());
    }
  }

  @Override
  @Transactional
  public void statusChangeNotification(String negotiationId, NegotiationEvent action) {
    performStatusChangeNofitication(negotiationId, action, null);
  }

  @Override
  @Transactional
  public void statusChangeNotification(String negotiationId, NegotiationEvent action, String post) {
    performStatusChangeNofitication(negotiationId, action, post);
  }

  private void performStatusChangeNofitication(
      String negotiationId, NegotiationEvent action, String post) {
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
            "Request status update",
            getMessage(action, post),
            NotificationEmailStatus.EMAIL_NOT_SENT);
    try {
      notification = notificationRepository.save(notification);
    } catch (PersistenceException e) {
      log.error("Error while saving notification %s".formatted(notification.getMessage()), e);
      return;
    }
    eventPublisher.publishEvent(
        new NewNotificationEvent(this, notification.getId(), "negotiation-status-change"));
  }
}
