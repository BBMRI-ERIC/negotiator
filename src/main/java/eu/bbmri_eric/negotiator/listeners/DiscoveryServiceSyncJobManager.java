package eu.bbmri_eric.negotiator.listeners;

import eu.bbmri_eric.negotiator.database.model.DiscoveryServiceSynchronizationJob;
import eu.bbmri_eric.negotiator.database.model.DiscoveryServiceSyncronizationJobStatus;
import eu.bbmri_eric.negotiator.database.repository.DiscoveryServiceSynchronizationJobRepository;
import eu.bbmri_eric.negotiator.events.DiscoveryServiceSynchronizationEvent;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@CommonsLog
@Component
public class DiscoveryServiceSyncJobManager
    implements ApplicationListener<DiscoveryServiceSynchronizationEvent> {
  @Autowired
  private DiscoveryServiceSynchronizationJobRepository discoveryServiceSynchronizationJobRepository;

  @Override
  public void onApplicationEvent(DiscoveryServiceSynchronizationEvent event) {
    log.info("Consuming spring custom event related to jobId - " + event.getJobId());
    log.info("Updating job status...");
    DiscoveryServiceSynchronizationJob job =
        discoveryServiceSynchronizationJobRepository.findDetailedById(event.getJobId()).get();
    job.setStatus(DiscoveryServiceSyncronizationJobStatus.IN_PROGRESS);
    discoveryServiceSynchronizationJobRepository.save(job);
  }
}
