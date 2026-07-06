package eu.bbmri_eric.negotiator.lifecycle.negotiation;

import eu.bbmri_eric.negotiator.lifecycle.statemachine.TransitionAction;
import eu.bbmri_eric.negotiator.negotiation.NegotiationService;
import org.springframework.stereotype.Component;

@Component("lifecycleDisablePostsAction")
public class DisablePostsAction implements TransitionAction<NegotiationTransitionContext> {

  private final NegotiationService negotiationService;

  public DisablePostsAction(NegotiationService negotiationService) {
    this.negotiationService = negotiationService;
  }

  @Override
  public void execute(NegotiationTransitionContext context) {
    negotiationService.setPublicPostsEnabled(context.negotiationId(), false);
    negotiationService.setPrivatePostsEnabled(context.negotiationId(), false);
  }
}
