package eu.bbmri_eric.negotiator.negotiation.state_machine.resource;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event depicting a change of the current state of a Resource in a Negotiation.
 *
 * <p>Carries its two States and its Event as names. Any of the three may be {@code null}. A
 * Resource with no recorded State has no origin, and an administrator setting a State directly has
 * no Transition behind it.
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
