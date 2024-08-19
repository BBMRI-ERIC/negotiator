package eu.bbmri_eric.negotiator.discovery_service;

public interface DiscoverySynchronizationJobService {

  DiscoverySyncJobServiceDTO createSyncJob(Long discoveryServiceId);
}
