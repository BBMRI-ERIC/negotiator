package eu.bbmri_eric.negotiator.service;

import eu.bbmri_eric.negotiator.database.model.DiscoveryService;
import eu.bbmri_eric.negotiator.database.model.DiscoveryServiceSynchronizationJob;
import eu.bbmri_eric.negotiator.database.model.DiscoveryServiceSyncronizationJobStatus;
import eu.bbmri_eric.negotiator.database.repository.DiscoveryServiceRepository;
import eu.bbmri_eric.negotiator.database.repository.DiscoveryServiceSynchronizationJobRepository;
import eu.bbmri_eric.negotiator.dto.discoverySyncJobservice.DiscoverySyncJobServiceCreateDTO;
import eu.bbmri_eric.negotiator.dto.discoverySyncJobservice.DiscoverySyncJobServiceDTO;
import eu.bbmri_eric.negotiator.publishers.DiscoveryServiceSynchronizationEventPublisher;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@CommonsLog
@Service
public class DiscoverySynchronizationJobServiceImpl implements DiscoverySynchronizationJobService {

  @Autowired DiscoveryServiceRepository discoveryServiceRepository;

  @Autowired DiscoveryServiceSynchronizationEventPublisher publisher;

  @Autowired
  DiscoveryServiceSynchronizationJobRepository discoveryServiceSynchronizationJobRepository;

  @Override
  public DiscoverySyncJobServiceDTO createSyncJob(
      DiscoverySyncJobServiceCreateDTO discoverySyncJobServiceCreateDTO) {
    String discoveryServiceName = discoverySyncJobServiceCreateDTO.getDiscoveryServiceName();
    Optional<DiscoveryService> discoveryService =
        discoveryServiceRepository.findByName(discoveryServiceName);
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
              "Sync Job %s properly instantiated for Discovery Service %s",
              job.getId(), discoveryServiceName);
      publisher.publishDiscoveryServiceSynchronizationEvent(discoveryServiceName);
      log.info(
          String.format(
              "Sync job event for discovery service %s properly published", discoveryServiceName));

      return DiscoverySyncJobServiceDTO.builder()
          .id(job.getId())
          .discoveryServiceName(discoveryServiceName)
          .creationDate(LocalDateTime.now())
          .modifiedDate(LocalDateTime.now())
          .message(message)
          .build();
    } else {
      String message =
          String.format(
              "Impossible to create sync job: no such discovery service with name",
              discoveryServiceName);
      log.error(message);
      throw new RuntimeException(message);
    }
  }
}
