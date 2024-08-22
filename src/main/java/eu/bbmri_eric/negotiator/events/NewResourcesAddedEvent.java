package eu.bbmri_eric.negotiator.events;

import eu.bbmri_eric.negotiator.database.model.Negotiation;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/** Event published when new resources have been added to a Negotiation. */
@Getter
public class NewResourcesAddedEvent extends ApplicationEvent {

  private final Negotiation negotiation;

  public NewResourcesAddedEvent(Object source, Negotiation negotiation) {
    super(source);
    this.negotiation = negotiation;
  }
}
