package eu.bbmri_eric.negotiator.negotiation;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class NewNegotiationEvent extends ApplicationEvent {
  private final String negotiationId;
  private final String currentState;

  public NewNegotiationEvent(Object source, String negotiationId, String currentState) {
    super(source);
    this.negotiationId = negotiationId;
    this.currentState = currentState;
  }
}
