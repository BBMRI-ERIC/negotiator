package eu.bbmri_eric.negotiator.service;

import eu.bbmri_eric.negotiator.dto.discoverySyncJobservice.DiscoverySyncJobServiceCreateDTO;
import eu.bbmri_eric.negotiator.dto.discoverySyncJobservice.DiscoverySyncJobServiceDTO;

public interface DiscoverySynchronizationJobService {

  DiscoverySyncJobServiceDTO createSyncJob(
      DiscoverySyncJobServiceCreateDTO discoverySyncJobServiceCreateDTO);
}
