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

  @Override
  @Transactional
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
            "Request status update",
            "The request was %sd by an Administrator and the representatives of respective organizations were contacted."
                .formatted(action.getLabel().toLowerCase()),
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
