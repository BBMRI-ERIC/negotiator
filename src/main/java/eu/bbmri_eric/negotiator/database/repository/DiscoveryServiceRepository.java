package eu.bbmri_eric.negotiator.database.repository;

import eu.bbmri_eric.negotiator.database.model.DiscoveryService;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DiscoveryServiceRepository extends JpaRepository<DiscoveryService, Long> {

  Optional<DiscoveryService> findByUrl(String url);

  Optional<DiscoveryService> findByName(String name);
}
