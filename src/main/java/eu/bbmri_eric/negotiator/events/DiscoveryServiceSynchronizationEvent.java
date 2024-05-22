package eu.bbmri_eric.negotiator.events;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

/** Event for Discovery Service synchronization * */
@Getter
@Setter
public class DiscoveryServiceSynchronizationEvent extends ApplicationEvent {

  private String jobId;
  private String discoveryServiceName;

  public DiscoveryServiceSynchronizationEvent(
      Object source, String jobId, String discoveryServiceName) {
    super(source);
    this.jobId = jobId;
    this.discoveryServiceName = discoveryServiceName;
  }
}
