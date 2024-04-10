package eu.bbmri_eric.negotiator.database.repository;

import eu.bbmri_eric.negotiator.configuration.state_machine.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.configuration.state_machine.resource.NegotiationResourceState;
import eu.bbmri_eric.negotiator.database.model.Negotiation;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface NegotiationRepository
    extends JpaRepository<Negotiation, String>, JpaSpecificationExecutor<Negotiation> {

  Optional<Negotiation> findDetailedById(String id);

  @Query(value = "select currentState from Negotiation where id = :id")
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
          "SELECT EXISTS (SELECT rs.id "
              + "FROM request rq JOIN request_resources_link rrl on rq.id = rrl.request_id "
              + "                JOIN resource rs on rs.id = rrl.resource_id "
              + "WHERE rq.negotiation_id = :negotiationId AND "
              + "      rs.id in ("
              + "         select rrl.resource_id "
              + "         from person p join resource_representative_link rrl ON p.id = rrl.person_id "
              + "         where p.id = :personId"
              + "))",
      nativeQuery = true)
  boolean isRepresentativeOfAnyResource(String negotiationId, Long personId);

  @Query(
      value =
          "SELECT EXISTS ("
              + "SELECT n.id "
              + "FROM negotiation n "
              + "WHERE n.id = :negotiationId AND "
              + "n.created_by = :personId)",
      nativeQuery = true)
  boolean isNegotiationCreator(String negotiationId, Long personId);
}
