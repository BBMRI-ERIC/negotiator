package eu.bbmri_eric.negotiator.database.repository;

import eu.bbmri_eric.negotiator.database.model.Network;
import eu.bbmri_eric.negotiator.database.model.Resource;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NetworkRepository extends JpaRepository<Network, Long> {

  Optional<Network> findByExternalId(String externalId);

  boolean existsByExternalId(@NotNull String externalId);

  boolean existsByUri(@NotNull String uri);


}
