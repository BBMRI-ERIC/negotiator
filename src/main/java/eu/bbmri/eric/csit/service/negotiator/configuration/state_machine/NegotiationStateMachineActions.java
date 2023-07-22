package eu.bbmri.eric.csit.service.negotiator.configuration.state_machine;

import eu.bbmri.eric.csit.service.negotiator.service.NegotiationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.statemachine.action.Action;

@Configuration
public class NegotiationStateMachineActions {

  private final NegotiationService negotiationService;

  @Autowired
  public NegotiationStateMachineActions(@Lazy NegotiationService negotiationService) {
    this.negotiationService = negotiationService;
  }

  public Action<String, String> enablePosts() {
    return stateContext -> {
      String negotiationId = stateContext.getStateMachine().getId();
      negotiationService.enablePosts(negotiationId);
    };
  }
}
