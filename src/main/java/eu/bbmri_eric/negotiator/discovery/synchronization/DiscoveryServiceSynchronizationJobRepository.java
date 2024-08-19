package eu.bbmri_eric.negotiator.discovery.synchronization;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface DiscoveryServiceSynchronizationJobRepository
    extends JpaRepository<DiscoveryServiceSynchronizationJob, String>,
        JpaSpecificationExecutor<DiscoveryServiceSynchronizationJob> {

  Optional<DiscoveryServiceSynchronizationJob> findDetailedById(String id);

  Optional<DiscoveryServiceSynchronizationJob> findByStatus(
      DiscoveryServiceSyncronizationJobStatus status);
}
