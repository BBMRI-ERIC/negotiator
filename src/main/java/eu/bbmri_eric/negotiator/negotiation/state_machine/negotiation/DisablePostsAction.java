package eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation;

import eu.bbmri_eric.negotiator.negotiation.NegotiationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

/** SpringStateMachine action for disabling posts in a Negotiation */
@Component
public class DisablePostsAction implements Action<String, String> {

  @Autowired @Lazy NegotiationService negotiationService;

  @Override
  public void execute(StateContext<String, String> stateContext) {
    String negotiationId =
        stateContext.getMessage().getHeaders().get("negotiationId", String.class);
    negotiationService.setPublicPostsEnabled(negotiationId, false);
    negotiationService.setPrivatePostsEnabled(negotiationId, false);
  }
}
