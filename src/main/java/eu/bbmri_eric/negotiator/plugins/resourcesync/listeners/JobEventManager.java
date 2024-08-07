package eu.bbmri_eric.negotiator.plugins.resourcesync.listeners;

import eu.bbmri_eric.negotiator.database.model.DiscoveryServiceSynchronizationJob;
import eu.bbmri_eric.negotiator.database.model.DiscoveryServiceSyncronizationJobStatus;
import eu.bbmri_eric.negotiator.database.repository.DiscoveryServiceSynchronizationJobRepository;
import eu.bbmri_eric.negotiator.events.DiscoveryServiceSynchronizationEvent;
import eu.bbmri_eric.negotiator.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.exceptions.EntityNotStorableException;
import eu.bbmri_eric.negotiator.service.DiscoveryServiceClient;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@CommonsLog
@Component
public class JobEventManager implements ApplicationListener<DiscoveryServiceSynchronizationEvent> {
  @Autowired
  private DiscoveryServiceSynchronizationJobRepository discoveryServiceSynchronizationJobRepository;

  @Autowired private DiscoveryServiceClient discoveryServiceSyncClient;

  @Override
  public void onApplicationEvent(DiscoveryServiceSynchronizationEvent event) {
    log.info("Consuming spring custom event related to jobId - " + event.getJobId());
    log.info("Updating job status...");
    DiscoveryServiceSynchronizationJob job =
        discoveryServiceSynchronizationJobRepository
            .findDetailedById(event.getJobId())
            .orElseThrow(() -> new EntityNotFoundException(event.getJobId()));
    new EntityNotFoundException(event.getJobId());
    job.setStatus(DiscoveryServiceSyncronizationJobStatus.IN_PROGRESS);
    DiscoveryServiceSynchronizationJob savedJob =
        discoveryServiceSynchronizationJobRepository.save(job);
    try {
      discoveryServiceSyncClient.syncAllOrganizations();
      discoveryServiceSyncClient.syncAllResources();
      savedJob.setStatus(DiscoveryServiceSyncronizationJobStatus.COMPLETED);
    } catch (EntityNotStorableException e) {
      savedJob.setStatus(DiscoveryServiceSyncronizationJobStatus.FAILED);
    }
  }
}
