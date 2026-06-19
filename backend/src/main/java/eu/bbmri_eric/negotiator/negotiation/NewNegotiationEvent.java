package eu.bbmri_eric.negotiator.negotiation;

import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationState;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class NewNegotiationEvent extends ApplicationEvent {
  private final String negotiationId;
  private final NegotiationState currentState;

  public NewNegotiationEvent(Object source, String negotiationId, NegotiationState currentState) {
    super(source);
    this.negotiationId = negotiationId;
    this.currentState = currentState;
  }
}
