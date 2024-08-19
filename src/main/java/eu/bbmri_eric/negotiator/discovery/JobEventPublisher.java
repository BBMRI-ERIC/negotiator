package eu.bbmri_eric.negotiator.discovery;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@CommonsLog
public class JobEventPublisher {
  @Autowired private ApplicationEventPublisher applicationEventPublisher;

  public void publishDiscoveryServiceSynchronizationEvent(
      final String jobId, final Long serviceId) {
    log.debug("Publishing custom event. ");
    DiscoveryServiceSynchronizationEvent discoveryServiceSynchronizationEvent =
        new DiscoveryServiceSynchronizationEvent(this, jobId, serviceId);
    log.debug(String.format("Publishing new sync event for service: %s ", serviceId));
    applicationEventPublisher.publishEvent(discoveryServiceSynchronizationEvent);
  }
}
