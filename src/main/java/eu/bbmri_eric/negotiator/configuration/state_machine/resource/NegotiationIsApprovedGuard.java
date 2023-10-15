package eu.bbmri_eric.negotiator.configuration.state_machine.resource;

import eu.bbmri_eric.negotiator.configuration.state_machine.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.service.NegotiationService;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.guard.Guard;
import org.springframework.stereotype.Component;

/**
 * SSM Guard for verifying that the Negotiation is ONGOING before any interaction can be made with
 * the resource SM.
 */
@Component
public class NegotiationIsApprovedGuard implements Guard<String, String> {

  @Autowired @Lazy NegotiationService negotiationService;

  @Override
  public boolean evaluate(StateContext<String, String> context) {
    String negotiationId = context.getMessage().getHeaders().get("negotiationId", String.class);
    return Objects.equals(
        NegotiationState.IN_PROGRESS.name(),
        negotiationService.findById(negotiationId, false).getStatus());
  }
}
