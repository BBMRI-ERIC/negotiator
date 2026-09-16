package eu.bbmri_eric.negotiator.negotiation;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class NegotiationPayloadUpdatedEvent extends ApplicationEvent {

  private final String negotiationId;

  public NegotiationPayloadUpdatedEvent(Object source, String negotiationId) {
    super(source);
    this.negotiationId = negotiationId;
  }
}
