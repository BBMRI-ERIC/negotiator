package eu.bbmri_eric.negotiator.plugins.resourcesync.schedulers;

import eu.bbmri_eric.negotiator.service.DiscoverySynchronizationJobService;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@CommonsLog
public class JobEventScheduler {

  @Autowired DiscoverySynchronizationJobService discoverySynchronizationJobService;

  @Value("${synchronization.discoveryServiceId}")
  private String synchronizationDiscoveryServiceId;

  @Scheduled(cron = "${synchronization.frequency}") // Cron expression for running every minute
  public void scheduleDiscoverySynchronizationJob() {
    try {
      discoverySynchronizationJobService.createSyncJob(
          Long.valueOf(synchronizationDiscoveryServiceId));
    } catch (RuntimeException e) {
      String message =
          String.format(
              "Impossible to create sync job: no such discovery service with id %s",
              synchronizationDiscoveryServiceId);
      log.error(message);
    }
  }
}
