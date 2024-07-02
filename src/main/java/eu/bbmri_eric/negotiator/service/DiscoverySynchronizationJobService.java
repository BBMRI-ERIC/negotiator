package eu.bbmri_eric.negotiator.service;

import eu.bbmri_eric.negotiator.dto.syncjobservice.DiscoverySyncJobServiceDTO;

public interface DiscoverySynchronizationJobService {

  DiscoverySyncJobServiceDTO createSyncJob(Long discoveryServiceId);
}
