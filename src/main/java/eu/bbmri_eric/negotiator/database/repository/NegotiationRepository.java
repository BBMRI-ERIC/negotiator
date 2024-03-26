package eu.bbmri_eric.negotiator.database.repository;

import eu.bbmri_eric.negotiator.database.model.Negotiation;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface NegotiationRepository
    extends JpaRepository<Negotiation, String>, JpaSpecificationExecutor<Negotiation> {

  @EntityGraph(value = "negotiation-with-detailed-children")
  Optional<Negotiation> findById(String id);

  Optional<Negotiation> findDetailedById(String id);

  boolean existsByIdAndCreatedBy_Id(String negotiationId, Long personId);
}
