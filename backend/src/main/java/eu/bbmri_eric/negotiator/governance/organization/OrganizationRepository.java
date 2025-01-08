package eu.bbmri_eric.negotiator.governance.organization;

import jakarta.validation.constraints.NotNull;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {

  @EntityGraph(value = "organization-with-detailed-resources")
  Optional<Organization> findDetailedById(Long id);

  @EntityGraph(value = "organization-with-detailed-resources")
  Optional<Organization> findByExternalId(String externalId);

  boolean existsByExternalId(@NotNull String externalId);
}
