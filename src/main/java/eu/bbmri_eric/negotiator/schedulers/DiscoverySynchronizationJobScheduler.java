package eu.bbmri_eric.negotiator.schedulers;

import eu.bbmri_eric.negotiator.dto.syncjobservice.DiscoverySyncJobServiceCreateDTO;
import eu.bbmri_eric.negotiator.service.DiscoverySynchronizationJobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DiscoverySynchronizationJobScheduler {

  @Autowired DiscoverySynchronizationJobService discoverySynchronizationJobService;

  @Value("${synchronization.discoveryServiceName}")
  private String synchronizationDiscoveryServiceName;

  @Scheduled(cron = "${synchronization.frequency}") // Cron expression for running every minute
  public void scheduleDiscoverySynchronizationJob() {
    DiscoverySyncJobServiceCreateDTO request =
        DiscoverySyncJobServiceCreateDTO.builder()
            .discoveryServiceName(synchronizationDiscoveryServiceName)
            .build();
    discoverySynchronizationJobService.createSyncJob(request);
  }
}
