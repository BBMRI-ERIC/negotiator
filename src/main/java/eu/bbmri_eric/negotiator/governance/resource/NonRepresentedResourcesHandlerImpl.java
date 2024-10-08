package eu.bbmri_eric.negotiator.governance.resource;

import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.NegotiationResourceEvent;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.NegotiationResourceState;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.ResourceLifecycleService;
import jakarta.transaction.Transactional;
import java.util.Objects;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.stereotype.Component;

@Component
@CommonsLog
public class NonRepresentedResourcesHandlerImpl implements NonRepresentedResourcesHandler {

  private final NegotiationRepository negotiationRepository;
  private final ResourceLifecycleService resourceLifecycleService;

  public NonRepresentedResourcesHandlerImpl(
      NegotiationRepository negotiationRepository,
      ResourceLifecycleService resourceLifecycleService) {
    this.negotiationRepository = negotiationRepository;
    this.resourceLifecycleService = resourceLifecycleService;
  }

  @Override
  @Transactional
  public void updateResourceInOngoingNegotiations(Long resourceId, String sourceId) {
    for (Negotiation negotiation :
        negotiationRepository.findAllByCurrentState(NegotiationState.IN_PROGRESS)) {
      NegotiationResourceState state;
      try {
        state = negotiation.getCurrentStateForResource(sourceId);
      } catch (IllegalArgumentException e) {
        continue;
      }
      if (Objects.equals(state, NegotiationResourceState.REPRESENTATIVE_UNREACHABLE)) {
        updateResourceStatus(negotiation.getId(), sourceId);
      }
    }
  }

  private void updateResourceStatus(String negotiationId, String sourceId) {
    NegotiationResourceState newStatus =
        resourceLifecycleService.sendEvent(
            negotiationId, sourceId, NegotiationResourceEvent.CONTACT);
    if (newStatus.equals(NegotiationResourceState.REPRESENTATIVE_CONTACTED)) {
      logSuccess(negotiationId, sourceId);
    } else {
      logError(negotiationId, sourceId);
    }
  }

  private static void logError(String negotiationId, String sourceId) {
    log.error(
        "LIFECYCLE_CHANGE: Resource %s in Negotiation %s could not be updated"
            .formatted(sourceId, negotiationId));
  }

  private static void logSuccess(String negotiationId, String sourceId) {
    log.info(
        "LIFECYCLE_CHANGE: Representative for Resource %s in Negotiation %s was contacted"
            .formatted(sourceId, negotiationId));
  }
}
