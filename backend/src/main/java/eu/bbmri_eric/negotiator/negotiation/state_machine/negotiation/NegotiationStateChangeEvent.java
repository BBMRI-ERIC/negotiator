package eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/** Event depicting a change of the current state of a Negotiation. */
@Getter
public class NegotiationStateChangeEvent extends ApplicationEvent {
  private final String negotiationId;
  private final NegotiationState fromState;
  private final NegotiationState toState;
  private final NegotiationEvent event;
  private final String post;

  public NegotiationStateChangeEvent(
      Object source,
      String negotiationId,
      NegotiationState fromState,
      NegotiationState toState,
      NegotiationEvent event,
      String post) {
    super(source);
    this.negotiationId = negotiationId;
    this.fromState = fromState;
    this.toState = toState;
    this.event = event;
    this.post = post;
  }
}
