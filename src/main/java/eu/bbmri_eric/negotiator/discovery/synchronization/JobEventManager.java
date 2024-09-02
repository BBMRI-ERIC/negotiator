package eu.bbmri_eric.negotiator.discovery.synchronization;

import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.common.exceptions.EntityNotStorableException;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;

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
      discoveryServiceSyncClient.syncAllNetworks();
      savedJob.setStatus(DiscoveryServiceSyncronizationJobStatus.COMPLETED);
      discoveryServiceSynchronizationJobRepository.save(savedJob);
    } catch (EntityNotStorableException e) {
      savedJob.setStatus(DiscoveryServiceSyncronizationJobStatus.FAILED);
      discoveryServiceSynchronizationJobRepository.save(savedJob);
    } catch (WebClientResponseException e) {
      savedJob.setStatus(DiscoveryServiceSyncronizationJobStatus.FAILED);
      discoveryServiceSynchronizationJobRepository.save(savedJob);
    }
  }
}
