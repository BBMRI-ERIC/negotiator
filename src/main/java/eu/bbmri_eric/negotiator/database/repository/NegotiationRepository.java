package eu.bbmri_eric.negotiator.database.repository;

import eu.bbmri_eric.negotiator.configuration.state_machine.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.configuration.state_machine.resource.NegotiationResourceState;
import eu.bbmri_eric.negotiator.database.model.Negotiation;
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
      "SELECT n.currentStatePerResource "
          + "FROM Negotiation n "
          + "JOIN n.currentStatePerResource currentState "
          + "WHERE n.id = :negotiationId AND KEY(currentState) = :resourceId")
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
              + "    JOIN request rq ON n.id = rq.negotiation_id "
              + "    JOIN request_resources_link rrl ON rrl.request_id = rq.id "
              + "    JOIN resource rs ON rrl.resource_id = rs.id "
              + "    JOIN organization o ON rs.organization_id = o.id "
              + "WHERE n.id = :negotiationId and o.external_id = :organizationExternalId)",
      nativeQuery = true)
  boolean isOrganizationPartOfNegotiation(String negotiationId, String organizationExternalId);

  @Query(
      value =
          "SELECT distinct (n) "
              + "FROM Negotiation n "
              + "JOIN n.requests rq "
              + "JOIN rq.resources rs "
              + "JOIN rs.networks net "
              + "WHERE net.id = :networkId")
  Page<Negotiation> findAllForNetwork(Long networkId, Pageable pageable);
}
