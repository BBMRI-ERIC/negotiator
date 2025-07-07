package eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation;

import eu.bbmri_eric.negotiator.governance.resource.Resource;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.NegotiationResourceState;
import jakarta.transaction.Transactional;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

/** SSM action for initializing states per Resource. */
@Component
@CommonsLog
public class InitializeStateForResourceAction implements Action<String, String> {

  @Autowired @Lazy NegotiationRepository negotiationRepository;

  @Override
  @Transactional
  public void execute(StateContext<String, String> context) {
    String negotiationId = context.getMessage().getHeaders().get("negotiationId", String.class);
    Negotiation negotiation = negotiationRepository.findDetailedById(negotiationId).orElseThrow();
    for (Resource resource : negotiation.getResources()) {
      negotiation.setStateForResource(resource.getSourceId(), NegotiationResourceState.SUBMITTED);
    }
    // TODO: Fix marking of reachable resources
  }
}
