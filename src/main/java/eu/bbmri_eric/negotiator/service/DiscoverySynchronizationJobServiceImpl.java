package eu.bbmri_eric.negotiator.service;

import eu.bbmri_eric.negotiator.database.model.DiscoveryService;
import eu.bbmri_eric.negotiator.database.model.DiscoveryServiceSynchronizationJob;
import eu.bbmri_eric.negotiator.database.model.DiscoveryServiceSyncronizationJobStatus;
import eu.bbmri_eric.negotiator.database.repository.DiscoveryServiceRepository;
import eu.bbmri_eric.negotiator.database.repository.DiscoveryServiceSynchronizationJobRepository;
import eu.bbmri_eric.negotiator.dto.syncjobservice.DiscoverySyncJobServiceDTO;
import eu.bbmri_eric.negotiator.plugins.resourcesync.publishers.JobEventPublisher;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@CommonsLog
@Service
public class DiscoverySynchronizationJobServiceImpl implements DiscoverySynchronizationJobService {

  @Autowired DiscoveryServiceRepository discoveryServiceRepository;

  @Autowired JobEventPublisher publisher;

  @Autowired
  DiscoveryServiceSynchronizationJobRepository discoveryServiceSynchronizationJobRepository;

  @Override
  public DiscoverySyncJobServiceDTO createSyncJob(Long jobId) {
    Optional<DiscoveryService> discoveryService = discoveryServiceRepository.findById(jobId);
    if (!discoveryService.isEmpty()) {
      LocalDateTime creationDate = LocalDateTime.now();
      LocalDateTime modifyDate = LocalDateTime.now();
      DiscoveryServiceSynchronizationJob job =
          DiscoveryServiceSynchronizationJob.builder()
              .service(discoveryService.get())
              .creationDate(creationDate)
              .modifiedDate(modifyDate)
              .status(DiscoveryServiceSyncronizationJobStatus.SUBMITTED)
              .build();

      log.info("Saving new job entity....");
      discoveryServiceSynchronizationJobRepository.save(job);

      String message =
          String.format(
              "Sync Job %s properly instantiated for Discovery Service %s", job.getId(), jobId);
      publisher.publishDiscoveryServiceSynchronizationEvent(job.getId(), jobId.toString());
      log.info(String.format("Sync job event for discovery service %s properly published", jobId));

      return DiscoverySyncJobServiceDTO.builder()
          .id(job.getId())
          .discoveryServiceName(discoveryService.get().getName())
          .creationDate(LocalDateTime.now())
          .modifiedDate(LocalDateTime.now())
          .message(message)
          .build();
    } else {
      String message =
          String.format(
              "Impossible to create sync job: no such discovery service with id %s", jobId);
      log.error(message);
      throw new RuntimeException(message);
    }
  }
}
