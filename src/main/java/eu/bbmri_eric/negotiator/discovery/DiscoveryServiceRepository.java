package eu.bbmri_eric.negotiator.discovery;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DiscoveryServiceRepository extends JpaRepository<DiscoveryService, Long> {

  Optional<DiscoveryService> findByUrl(String url);
}
