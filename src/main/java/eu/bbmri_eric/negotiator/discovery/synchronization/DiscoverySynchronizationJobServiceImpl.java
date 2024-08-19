package eu.bbmri_eric.negotiator.discovery.synchronization;

import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.discovery.DiscoveryService;
import eu.bbmri_eric.negotiator.discovery.DiscoveryServiceRepository;
import java.time.LocalDateTime;
import lombok.extern.apachecommons.CommonsLog;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@CommonsLog
@Service
public class DiscoverySynchronizationJobServiceImpl implements DiscoverySynchronizationJobService {

  @Autowired DiscoveryServiceRepository discoveryServiceRepository;

  @Autowired JobEventPublisher publisher;

  @Autowired
  DiscoveryServiceSynchronizationJobRepository discoveryServiceSynchronizationJobRepository;

  ModelMapper modelMapper = new ModelMapper();

  @Override
  public DiscoverySyncJobServiceDTO createSyncJob(Long serviceId) {
    DiscoveryService discoveryService =
        discoveryServiceRepository
            .findById(serviceId)
            .orElseThrow(() -> new EntityNotFoundException(serviceId));
    LocalDateTime creationDate, modifyDate;
    creationDate = modifyDate = LocalDateTime.now();
    DiscoveryServiceSynchronizationJob job =
        new DiscoveryServiceSynchronizationJob(
            null,
            discoveryService,
            creationDate,
            modifyDate,
            DiscoveryServiceSyncronizationJobStatus.SUBMITTED);
    log.debug("Saving new job entity....");
    discoveryServiceSynchronizationJobRepository.save(job);
    publisher.publishDiscoveryServiceSynchronizationEvent(job.getId(), serviceId);
    log.debug(
        String.format("Sync job event for discovery service %s properly published", serviceId));

    return modelMapper.map(job, DiscoverySyncJobServiceDTO.class);
  }
}
