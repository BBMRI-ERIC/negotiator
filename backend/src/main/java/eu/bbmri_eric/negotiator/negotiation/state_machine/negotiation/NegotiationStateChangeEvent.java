package eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event depicting a change of the current state of a Negotiation.
 *
 * <p>Names its two States and its Event as bare strings, so a consumer can read the payload without
 * naming a type the Lifecycle redesign deletes. {@code null} is a real value on all three: a
 * Transition whose trigger cannot be resolved carries no Event.
 */
@Getter
public class NegotiationStateChangeEvent extends ApplicationEvent {
  private final String negotiationId;
  private final String fromState;
  private final String toState;
  private final String event;

  public NegotiationStateChangeEvent(
      Object source, String negotiationId, String fromState, String toState, String event) {
    super(source);
    this.negotiationId = negotiationId;
    this.fromState = fromState;
    this.toState = toState;
    this.event = event;
  }
}
