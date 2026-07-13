package eu.bbmri_eric.negotiator.lifecycle.resource;

import eu.bbmri_eric.negotiator.lifecycle.NegotiatorTransitionContext;
import eu.bbmri_eric.negotiator.lifecycle.statemachine.Guard;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.negotiation.NegotiationState;
import org.springframework.stereotype.Component;

/** Machine guard that allows events only when the parent negotiation is in progress. */
@Component("negotiationInProgress")
public class NegotiationInProgressGuard implements Guard<NegotiatorTransitionContext> {

  private final NegotiationRepository negotiationRepository;

  public NegotiationInProgressGuard(NegotiationRepository negotiationRepository) {
    this.negotiationRepository = negotiationRepository;
  }

  @Override
  public boolean evaluate(NegotiatorTransitionContext context) {
    return negotiationRepository
        .findNegotiationStateById(context.negotiationId())
        .map(NegotiationState.IN_PROGRESS::equals)
        .orElse(false);
  }
}
