package eu.bbmri_eric.negotiator.events;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

/** Event for Discovery Service synchronization * */
@Getter
@Setter
public class DiscoveryServiceSynchronizationEvent extends ApplicationEvent {

  private String discoveryServiceName;

  public DiscoveryServiceSynchronizationEvent(Object source, String discoveryServiceName) {
    super(source);
    this.discoveryServiceName = discoveryServiceName;
  }
}
