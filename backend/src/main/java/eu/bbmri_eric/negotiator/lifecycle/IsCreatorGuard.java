package eu.bbmri_eric.negotiator.lifecycle;

import eu.bbmri_eric.negotiator.lifecycle.statemachine.Guard;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import org.springframework.stereotype.Component;

/** Guard that allows only the creator of the negotiation in context. */
@Component("isCreator")
public class IsCreatorGuard implements Guard<NegotiatorTransitionContext> {

  private final NegotiationRepository negotiationRepository;

  public IsCreatorGuard(NegotiationRepository negotiationRepository) {
    this.negotiationRepository = negotiationRepository;
  }

  @Override
  public boolean evaluate(NegotiatorTransitionContext context) {
    if (context.userId() == null) {
      return false;
    }
    return negotiationRepository.existsByIdAndCreatedBy_Id(
        context.negotiationId(), context.userId());
  }
}
