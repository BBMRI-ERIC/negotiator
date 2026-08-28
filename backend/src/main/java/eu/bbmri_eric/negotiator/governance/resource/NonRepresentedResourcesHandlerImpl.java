package eu.bbmri_eric.negotiator.governance.resource;

import eu.bbmri_eric.negotiator.lifecycle.WellKnownNegotiationStates;
import eu.bbmri_eric.negotiator.lifecycle.WellKnownResourceStates;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import jakarta.transaction.Transactional;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.stereotype.Component;

@Component
@CommonsLog
public class NonRepresentedResourcesHandlerImpl implements NonRepresentedResourcesHandler {

  private final NegotiationRepository negotiationRepository;

  public NonRepresentedResourcesHandlerImpl(NegotiationRepository negotiationRepository) {
    this.negotiationRepository = negotiationRepository;
  }

  @Override
  @Transactional
  public void updateResourceInOngoingNegotiations(Long resourceId, String sourceId) {
    for (Negotiation negotiation :
        negotiationRepository.findAllByCurrentState(WellKnownNegotiationStates.IN_PROGRESS)) {
      String state;
      try {
        state = negotiation.getCurrentStateForResource(sourceId);
      } catch (IllegalArgumentException e) {
        continue;
      }
      if (WellKnownResourceStates.REPRESENTATIVE_UNREACHABLE.equals(state)) {
        negotiation.setStateForResource(sourceId, WellKnownResourceStates.REPRESENTATIVE_CONTACTED);
        // TODO: add call for notifying the representative
      }
    }
  }
}
