package eu.bbmri_eric.negotiator.governance.organization;

import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.NegotiationResourceState;
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

  @Query(
      value =
          """
                  SELECT nrl.current_state
                  FROM negotiation_resource_link nrl
                      INNER JOIN resource r ON nrl.resource_id = r.id
                  WHERE r.organization_id = :organizationId
                      AND nrl.negotiation_id = :negotiationId
                  ORDER BY
                      CASE nrl.current_state
                          WHEN 'SUBMITTED' THEN 0
                          WHEN 'REPRESENTATIVE_UNREACHABLE' THEN 1
                          WHEN 'REPRESENTATIVE_CONTACTED' THEN 2
                          WHEN 'RETURNED_FOR_RESUBMISSION' THEN 3
                          WHEN 'CHECKING_AVAILABILITY' THEN 4
                          WHEN 'RESOURCE_UNAVAILABLE_WILLING_TO_COLLECT' THEN 5
                          WHEN 'RESOURCE_UNAVAILABLE' THEN 6
                          WHEN 'RESOURCE_AVAILABLE' THEN 7
                          WHEN 'ACCESS_CONDITIONS_INDICATED' THEN 8
                          WHEN 'ACCESS_CONDITIONS_MET' THEN 9
                          WHEN 'RESOURCE_NOT_MADE_AVAILABLE' THEN 10
                          WHEN 'RESOURCE_MADE_AVAILABLE' THEN 11
                      END DESC
                  LIMIT 1;
                  """,
          nativeQuery = true)
  NegotiationResourceState getCurrentOrganizationState(Long organizationId, String negotiationId);

}
