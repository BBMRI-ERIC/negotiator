package eu.bbmri.eric.csit.service.negotiator.configuration.state_machine;

import eu.bbmri.eric.csit.service.negotiator.service.NegotiationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

@Component
public class EnablePostsAction implements Action<String, String> {

  @Autowired @Lazy NegotiationService negotiationService;

  @Override
  public void execute(StateContext<String, String> stateContext) {
    String negotiationId =
        stateContext.getMessage().getHeaders().get("negotiationId", String.class);
    negotiationService.enablePosts(negotiationId);
  }
}
