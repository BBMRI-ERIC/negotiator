package eu.bbmri_eric.negotiator.notification.internal;

import eu.bbmri_eric.negotiator.lifecycle.WellKnownNegotiationStates;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationStateChangeEvent;
import jakarta.transaction.Transactional;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.stereotype.Component;

/**
 * Handles negotiations moving to in-progress status by updating resource states and notifying
 * representatives.
 */
@Component
@CommonsLog
class NegotiationInProgressHandler implements NotificationStrategy<NegotiationStateChangeEvent> {

  private final ResourceNotificationService resourceNotificationService;

  NegotiationInProgressHandler(ResourceNotificationService resourceNotificationService) {
    this.resourceNotificationService = resourceNotificationService;
  }

  @Override
  public Class<NegotiationStateChangeEvent> getSupportedEventType() {
    return NegotiationStateChangeEvent.class;
  }

  @Override
  @Transactional
  public void notify(NegotiationStateChangeEvent event) {
    if (WellKnownNegotiationStates.IN_PROGRESS.equals(event.getToState())) {
      resourceNotificationService.notifyResourceRepresentatives(event.getNegotiationId());
    }
  }
}
