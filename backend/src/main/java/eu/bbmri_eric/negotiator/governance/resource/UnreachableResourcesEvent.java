package eu.bbmri_eric.negotiator.governance.resource;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class UnreachableResourcesEvent extends ApplicationEvent {
  private final int numberOFUnreachableResources;
  private final String negotiationId;

  public UnreachableResourcesEvent(
      Object source, int numberOFUnreachableResources, String negotiationId) {
    super(source);
    this.numberOFUnreachableResources = numberOFUnreachableResources;
    this.negotiationId = negotiationId;
  }
}
