package eu.bbmri_eric.negotiator.lifecycle.negotiation;

import eu.bbmri_eric.negotiator.lifecycle.statemachine.TransitionAction;
import eu.bbmri_eric.negotiator.negotiation.NegotiationService;
import org.springframework.stereotype.Component;

@Component("enablePublicPosts")
public class EnablePublicPostsAction implements TransitionAction<NegotiationTransitionContext> {

  private final NegotiationService negotiationService;

  public EnablePublicPostsAction(NegotiationService negotiationService) {
    this.negotiationService = negotiationService;
  }

  @Override
  public void execute(NegotiationTransitionContext context) {
    negotiationService.setPublicPostsEnabled(context.negotiationId(), true);
  }
}
