package eu.bbmri_eric.negotiator.database.repository;

import eu.bbmri_eric.negotiator.database.model.Network;
import jakarta.validation.constraints.NotNull;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NetworkRepository extends JpaRepository<Network, Long> {

  Optional<Network> findDetailedById(Long id);

  Optional<Network> findByExternalId(String externalId);

  boolean existsByExternalId(@NotNull String externalId);

  boolean existsByUri(@NotNull String uri);
}
