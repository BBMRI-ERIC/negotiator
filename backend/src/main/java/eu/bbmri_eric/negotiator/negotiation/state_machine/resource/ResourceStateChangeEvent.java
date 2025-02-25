package eu.bbmri_eric.negotiator.negotiation.state_machine.resource;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class ResourceStateChangeEvent extends ApplicationEvent {
  private final String negotiationId;
  private final String resourceId;
  private final NegotiationResourceState fromState;
  private final NegotiationResourceState toState;

  public ResourceStateChangeEvent(
      Object source,
      String negotiationId,
      String resourceId,
      NegotiationResourceState fromState,
      NegotiationResourceState toState) {
    super(source);
    this.negotiationId = negotiationId;
    this.resourceId = resourceId;
    this.fromState = fromState;
    this.toState = toState;
  }
}
