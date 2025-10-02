package eu.bbmri_eric.negotiator.negotiation;

import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.NegotiationResourceState;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface NegotiationRepository
    extends JpaRepository<Negotiation, String>, JpaSpecificationExecutor<Negotiation> {

  Optional<Negotiation> findDetailedById(String id);

  @Query(value = "SELECT currentState from Negotiation where id = :id")
  Optional<NegotiationState> findNegotiationStateById(String id);

  List<Negotiation> findAllByCurrentState(NegotiationState state);

  @Query(
      value =
          """
            SELECT PERCENTILE_CONT(0.5)
           WITHIN GROUP (ORDER BY EXTRACT(EPOCH FROM (nrlr.creation_date - n.creation_date)) / 86400)
           AS median_days
            FROM Negotiation n
            LEFT JOIN public.negotiation_resource_lifecycle_record nrlr on n.id = nrlr.negotiation_id
            left join public.network_resources_link nrl on nrlr.resource_id = nrl.resource_id
            where nrl.network_id = :networkId and (nrlr.changed_to = 'CHECKING_AVAILABILITY' or nrlr.changed_to = 'RESOURCE_UNAVAILABLE')
                    and Date(n.creation_date) > :since and Date(n.creation_date) <= :until
        """,
      nativeQuery = true)
  Double getMedianResponseForNetwork(LocalDate since, LocalDate until, Long networkId);

  @Query(
      value =
          """
          SELECT rl.currentState
          FROM Negotiation n
          JOIN n.resourcesLink rl
          JOIN rl.id.resource
          WHERE n.id = :negotiationId AND rl.id.resource.sourceId = :resourceId
        """)
  Optional<NegotiationResourceState> findNegotiationResourceStateById(
      String negotiationId, String resourceId);

  boolean existsByIdAndCreatedBy_Id(String negotiationId, Long personId);

  @Query(
      value =
          "SELECT EXISTS ("
              + "SELECT distinct(n.id) "
              + "FROM negotiation n "
              + "    JOIN negotiation_resource_link rrl ON rrl.negotiation_id = n.id "
              + "    JOIN resource rs ON rrl.resource_id = rs.id "
              + "    JOIN organization o ON rs.organization_id = o.id "
              + "WHERE n.id = :negotiationId and o.external_id = :organizationExternalId)",
      nativeQuery = true)
  boolean isOrganizationPartOfNegotiation(String negotiationId, String organizationExternalId);

  @Query(
      value =
          "select count (distinct n.id) "
              + "FROM Negotiation n "
              + "JOIN n.resourcesLink rl "
              + "JOIN rl.id.resource rs "
              + "JOIN rs.networks net "
              + "WHERE net.id = :networkId and n.currentState != DRAFT and "
              + "DATE(n.creationDate) > :since and DATE(n.creationDate) <= :until")
  Integer countAllNotDraftForNetwork(LocalDate since, LocalDate until, Long networkId);

  @Query(value = "SELECT n FROM Negotiation n WHERE FUNCTION('DATE', n.creationDate) = :targetDate")
  Set<Negotiation> findAllCreatedOn(LocalDateTime targetDate);

  @Query(
      value =
          "SELECT distinct(n.id) "
              + "FROM negotiation n "
              + "    JOIN negotiation_resource_link rrl ON rrl.negotiation_id = n.id "
              + "    JOIN resource rs ON rrl.resource_id = rs.id "
              + "WHERE rrl.resource_id = :resourceId",
      nativeQuery = true)
  List<String> getNegotiationsByResource(Long resourceId);
}
