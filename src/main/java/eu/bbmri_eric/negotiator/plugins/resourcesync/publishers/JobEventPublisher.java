package eu.bbmri_eric.negotiator.plugins.resourcesync.publishers;

import eu.bbmri_eric.negotiator.events.DiscoveryServiceSynchronizationEvent;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@CommonsLog
public class JobEventPublisher {
  @Autowired private ApplicationEventPublisher applicationEventPublisher;

  public void publishDiscoveryServiceSynchronizationEvent(
      final String jobId, final String serviceName) {
    System.out.println("Publishing custom event. ");
    DiscoveryServiceSynchronizationEvent discoveryServiceSynchronizationEvent =
        new DiscoveryServiceSynchronizationEvent(this, jobId, serviceName);
    log.info(String.format("Publishing new sync event for service: %s ", serviceName));
    applicationEventPublisher.publishEvent(discoveryServiceSynchronizationEvent);
  }
}
