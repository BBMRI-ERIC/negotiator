package eu.bbmri_eric.negotiator.governance.network;

import eu.bbmri_eric.negotiator.database.model.Person;
import jakarta.validation.constraints.NotNull;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NetworkRepository extends JpaRepository<Network, Long> {

  Optional<Network> findByExternalId(String externalId);

  boolean existsByExternalId(@NotNull String externalId);

  boolean existsByUri(@NotNull String uri);

  Page<Network> findAllByManagersContains(Person manager, Pageable pageable);
}
