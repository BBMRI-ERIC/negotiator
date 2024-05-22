package eu.bbmri_eric.negotiator.listeners;

import eu.bbmri_eric.negotiator.events.DiscoveryServiceSynchronizationEvent;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@CommonsLog
@Component
public class DiscoveryServiceSynchronizationEventListener
    implements ApplicationListener<DiscoveryServiceSynchronizationEvent> {
  @Override
  public void onApplicationEvent(DiscoveryServiceSynchronizationEvent event) {
    log.info("Received spring custom event - " + event.getDiscoveryServiceName());
  }
}
