package eu.bbmri_eric.negotiator.plugins.resourcesync.listeners;

import eu.bbmri_eric.negotiator.database.model.DiscoveryServiceSynchronizationJob;
import eu.bbmri_eric.negotiator.database.model.DiscoveryServiceSyncronizationJobStatus;
import eu.bbmri_eric.negotiator.database.repository.DiscoveryServiceSynchronizationJobRepository;
import eu.bbmri_eric.negotiator.events.DiscoveryServiceSynchronizationEvent;
import eu.bbmri_eric.negotiator.exceptions.EntityNotStorableException;
import eu.bbmri_eric.negotiator.service.BBMRIDirectoryServiceSynchClient;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@CommonsLog
@Component
public class JobEventManager implements ApplicationListener<DiscoveryServiceSynchronizationEvent> {
  @Autowired
  private DiscoveryServiceSynchronizationJobRepository discoveryServiceSynchronizationJobRepository;

  @Autowired private BBMRIDirectoryServiceSynchClient bbmriDirectoryServiceSyncClient;

  @Override
  public void onApplicationEvent(DiscoveryServiceSynchronizationEvent event) {
    log.info("Consuming spring custom event related to jobId - " + event.getJobId());
    log.info("Updating job status...");
    DiscoveryServiceSynchronizationJob job =
        discoveryServiceSynchronizationJobRepository.findDetailedById(event.getJobId()).get();
    job.setStatus(DiscoveryServiceSyncronizationJobStatus.IN_PROGRESS);
    DiscoveryServiceSynchronizationJob savedJob =
        discoveryServiceSynchronizationJobRepository.save(job);
    try {
      bbmriDirectoryServiceSyncClient.syncAllDiscoveryServiceObjects();
      savedJob.setStatus(DiscoveryServiceSyncronizationJobStatus.COMPLETED);
    } catch (EntityNotStorableException e) {
      savedJob.setStatus(DiscoveryServiceSyncronizationJobStatus.FAILED);

    } finally {
      discoveryServiceSynchronizationJobRepository.save(job);
    }
  }
}
