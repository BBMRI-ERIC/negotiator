package eu.bbmri_eric.negotiator.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/** Event published when new resources have been added to a Negotiation. */
@Getter
public class NewResourcesAddedEvent extends ApplicationEvent {

  private final String negotiationId;

  public NewResourcesAddedEvent(Object source, String negotiationId) {
    super(source);
    this.negotiationId = negotiationId;
  }
}
