package eu.bbmri_eric.negotiator.discovery.synchronization;

public interface DiscoverySynchronizationJobService {

  DiscoverySyncJobServiceDTO createSyncJob(Long discoveryServiceId);

  DiscoverySyncJobServiceDTO updateSyncJob(
      Long discoveryServiceId, String jobId, DiscoverySyncJobServiceUpdateDTO request);
}
