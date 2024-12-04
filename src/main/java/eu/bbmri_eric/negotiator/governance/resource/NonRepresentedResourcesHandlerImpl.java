package eu.bbmri_eric.negotiator.governance.resource;

import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationState;
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
        negotiation.setStateForResource(
            sourceId, NegotiationResourceState.REPRESENTATIVE_CONTACTED);
        // TODO: add call for notifying the representative
      }
    }
  }
}
