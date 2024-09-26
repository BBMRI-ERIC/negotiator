package eu.bbmri_eric.negotiator.negotiation;

import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.NegotiationResourceState;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
      """
                  SELECT rl.currentState
                  FROM Negotiation n
                  JOIN n.resourcesLink rl
                  JOIN rl.id.resource
                  WHERE n.id = :negotiationId AND rl.id.resource.sourceId = :resourceId""")
  Optional<NegotiationResourceState> findNegotiationResourceStateById(
      String negotiationId, String resourceId);

  boolean existsByIdAndCreatedBy_Id(String negotiationId, Long personId);

  List<Negotiation> findByModifiedDateBeforeAndCurrentState(
      LocalDateTime thresholdTime, NegotiationState currentState);

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
          "SELECT distinct (n) "
              + "FROM Negotiation n "
              + "JOIN n.resourcesLink rl "
              + "JOIN rl.id.resource rs "
              + "JOIN rs.networks net "
              + "WHERE net.id = :networkId")
  Page<Negotiation> findAllForNetwork(Long networkId, Pageable pageable);

  @Query(
      value =
          "select count (distinct n.id) "
              + "FROM Negotiation n "
              + "JOIN n.resourcesLink rl "
              + "JOIN rl.id.resource rs "
              + "JOIN rs.networks net "
              + "WHERE net.id = :networkId")
  Integer countAllForNetwork(Long networkId);

  @Query(
      value =
          "select n.currentState, COUNT ( distinct n.id)"
              + "FROM Negotiation n "
              + "JOIN n.resourcesLink rl "
              + "JOIN rl.id.resource rs "
              + "JOIN rs.networks net "
              + "WHERE net.id = :networkId group by n.currentState")
  List<Object[]> countStatusDistribution(Long networkId);
}
