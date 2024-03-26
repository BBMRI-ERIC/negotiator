package eu.bbmri_eric.negotiator.database.repository;

import eu.bbmri_eric.negotiator.configuration.state_machine.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.configuration.state_machine.resource.NegotiationResourceState;
import eu.bbmri_eric.negotiator.database.model.Negotiation;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface NegotiationRepository
    extends JpaRepository<Negotiation, String>, JpaSpecificationExecutor<Negotiation> {

  @EntityGraph(value = "negotiation-with-detailed-children")
  Optional<Negotiation> findById(String id);

  Optional<Negotiation> findDetailedById(String id);

  @Query(value = "SELECT currentState from Negotiation where id = :id")
  Optional<NegotiationState> findNegotiationStateById(String id);

  @Query(
      "SELECT n.currentStatePerResource "
          + "FROM Negotiation n "
          + "JOIN n.currentStatePerResource currentState "
          + "WHERE n.id = :negotiationId AND KEY(currentState) = :resourceId")
  Optional<NegotiationResourceState> findNegotiationResourceStateById(
      String negotiationId, String resourceId);

  boolean existsByIdAndCreatedBy_Id(String negotiationId, Long personId);

  @Query(
      value =
          "SELECT EXISTS ("
              + "SELECT n.id "
              + "FROM negotiation n "
              + "WHERE n.id = :negotiationId AND "
              + "n.created_by = :personId)",
      nativeQuery = true)
  boolean isNegotiationCreator(String negotiationId, Long personId);

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
}
