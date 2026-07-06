package eu.bbmri_eric.negotiator.lifecycle.negotiation;

import eu.bbmri_eric.negotiator.lifecycle.statemachine.TransitionAction;
import eu.bbmri_eric.negotiator.negotiation.NegotiationService;
import org.springframework.stereotype.Component;

@Component("lifecycleEnablePrivatePostsAction")
public class EnablePrivatePostsAction implements TransitionAction<NegotiationTransitionContext> {

  private final NegotiationService negotiationService;

  public EnablePrivatePostsAction(NegotiationService negotiationService) {
    this.negotiationService = negotiationService;
  }

  @Override
  public void execute(NegotiationTransitionContext context) {
    negotiationService.setPrivatePostsEnabled(context.negotiationId(), true);
  }
}
