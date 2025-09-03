package eu.bbmri_eric.negotiator.governance.organization;

import jakarta.validation.constraints.NotNull;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface OrganizationRepository
    extends JpaRepository<Organization, Long>, JpaSpecificationExecutor<Organization> {

  @EntityGraph(value = "organization-with-detailed-resources")
  Optional<Organization> findDetailedById(Long id);

  @EntityGraph(value = "organization-with-detailed-resources")
  Optional<Organization> findByExternalId(String externalId);

  boolean existsByExternalId(@NotNull String externalId);

  @Query(
      value =
          """
                    SELECT DISTINCT o.*
                    FROM person p
                        INNER JOIN resource_representative_link rrl ON p.id = rrl.person_id
                        INNER JOIN resource r ON rrl.resource_id = r.id
                        INNER JOIN organization o ON r.organization_id = o.id
                    WHERE p.id = :personId
                        AND (:withdrawn IS NULL OR o.withdrawn = :withdrawn)
                        AND LOWER(o.name) LIKE LOWER(CONCAT('%', :name, '%'));
                  """,
      nativeQuery = true)
  Set<Organization> findAllOrganizationsContainingResourceRepresentedByUser(
      Long personId, String name, Boolean withdrawn);
}
