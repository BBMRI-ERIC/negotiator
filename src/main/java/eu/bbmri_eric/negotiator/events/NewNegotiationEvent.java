package eu.bbmri_eric.negotiator.events;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

@Getter
@Setter
public class NewNegotiationEvent extends ApplicationEvent {
  private String negotiationId;

  public NewNegotiationEvent(Object source) {
    super(source);
  }

  public NewNegotiationEvent(Object source, String negotiationId) {
    super(source);
    this.negotiationId = negotiationId;
  }
}
