package eu.bbmri_eric.negotiator.discovery.synchronization;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

/** Event for Discovery Service synchronization * */
@Getter
@Setter
public class DiscoveryServiceSynchronizationEvent extends ApplicationEvent {

  private String jobId;
  private Long discoveryServiceId;

  public DiscoveryServiceSynchronizationEvent(
      Object source, String jobId, Long discoveryServiceName) {
    super(source);
    this.jobId = jobId;
    this.discoveryServiceId = discoveryServiceId;
  }
}
