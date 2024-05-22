package eu.bbmri_eric.negotiator.publishers;

import eu.bbmri_eric.negotiator.events.DiscoveryServiceSynchronizationEvent;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@CommonsLog
public class DiscoveryServiceSynchronizationEventPublisher {
  @Autowired private ApplicationEventPublisher applicationEventPublisher;

  public void publishDiscoveryServiceSynchronizationEvent(final String serviceName) {
    System.out.println("Publishing custom event. ");
    DiscoveryServiceSynchronizationEvent discoveryServiceSynchronizationEvent =
        new DiscoveryServiceSynchronizationEvent(this, serviceName);
    log.info(String.format("Publishing new sync event for service: %s ", serviceName));
    applicationEventPublisher.publishEvent(discoveryServiceSynchronizationEvent);
  }
}
