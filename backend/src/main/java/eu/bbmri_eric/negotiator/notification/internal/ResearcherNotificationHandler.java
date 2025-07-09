package eu.bbmri_eric.negotiator.notification.internal;

import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationStateChangeEvent;
import eu.bbmri_eric.negotiator.notification.NotificationCreateDTO;
import eu.bbmri_eric.negotiator.notification.NotificationService;
import jakarta.transaction.Transactional;
import java.util.List;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.stereotype.Component;

/** Handles notifications when negotiation status changes, informing researchers of updates. */
@Component
@CommonsLog
class NegotiationStatusChangeHandler implements NotificationStrategy<NegotiationStateChangeEvent> {

  private final NotificationService notificationService;
  private final NegotiationRepository negotiationRepository;

  NegotiationStatusChangeHandler(
      NotificationService notificationService, NegotiationRepository negotiationRepository) {
    this.notificationService = notificationService;
    this.negotiationRepository = negotiationRepository;
  }

  @Override
  public Class<NegotiationStateChangeEvent> getSupportedEventType() {
    return NegotiationStateChangeEvent.class;
  }

  @Override
  @Transactional
  public void notify(NegotiationStateChangeEvent event) {
    log.error("called");
    switch (event.getChangedTo()) {
      case SUBMITTED -> createConfirmationNotification(event.getNegotiationId());
      case APPROVED, DECLINED, ABANDONED -> createStatusChangeNotification(event);
    }
  }

  private void createConfirmationNotification(String negotiationId) {
    log.error("here");
    Negotiation negotiation = negotiationRepository.findById(negotiationId).orElse(null);
    if (negotiation == null) {
      log.warn("Could not find negotiation with ID: " + negotiationId);
      return;
    }

    NotificationCreateDTO notification =
        new NotificationCreateDTO(
            List.of(negotiation.getCreatedBy().getId()),
            "Negotiation Submission Confirmed",
            "Your negotiation request has been successfully submitted and is now under review. You will be notified of any updates.",
            negotiationId);

    notificationService.createNotifications(notification);
    log.info("Sent confirmation notification to researcher for negotiation: " + negotiationId);
  }

  private void createStatusChangeNotification(NegotiationStateChangeEvent event) {
    Negotiation negotiation = negotiationRepository.findById(event.getNegotiationId()).orElse(null);
    if (negotiation == null) {
      log.warn("Could not find negotiation with ID: " + event.getNegotiationId());
      return;
    }

    String title = "Negotiation Status Update";
    String message = createStatusChangeMessage(event.getChangedTo(), negotiation.getTitle());

    NotificationCreateDTO notification =
        new NotificationCreateDTO(
            List.of(negotiation.getCreatedBy().getId()), title, message, event.getNegotiationId());

    notificationService.createNotifications(notification);
    log.info(
        "Sent status change notification to researcher for negotiation: "
            + event.getNegotiationId()
            + " - new status: "
            + event.getChangedTo());
  }

  private String createStatusChangeMessage(NegotiationState newState, String negotiationTitle) {
    return switch (newState) {
      case APPROVED ->
          "Your negotiation request '"
              + negotiationTitle
              + "' has been approved. You can now proceed with the next steps.";
      case DECLINED ->
          "Your negotiation request '"
              + negotiationTitle
              + "' has been rejected. Please review the feedback and consider resubmitting with modifications.";
      case ABANDONED ->
          "Your negotiation request '" + negotiationTitle + "' has been marked as abandoned.";
      default ->
          "Your negotiation request '"
              + negotiationTitle
              + "' status has been updated to: "
              + newState;
    };
  }
}
