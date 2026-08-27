package eu.bbmri_eric.negotiator.negotiation.state_machine.resource;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event depicting a change of the current state of a Resource in a Negotiation.
 *
 * <p>Names its two States and its Event as bare strings, so a consumer can read the payload without
 * naming a type the Lifecycle redesign deletes. {@code null} is a real value on all three: a
 * Resource with no recorded State yet has no origin, and the second producer of this event - an
 * administrator's direct state change - has no Transition behind it.
 */
@Getter
public class ResourceStateChangeEvent extends ApplicationEvent {
  private final String negotiationId;
  private final String resourceId;
  private final String fromState;
  private final String toState;
  private final String event;

  public ResourceStateChangeEvent(
      Object source,
      String negotiationId,
      String resourceId,
      String fromState,
      String toState,
      String event) {
    super(source);
    this.negotiationId = negotiationId;
    this.resourceId = resourceId;
    this.fromState = fromState;
    this.toState = toState;
    this.event = event;
  }
}
