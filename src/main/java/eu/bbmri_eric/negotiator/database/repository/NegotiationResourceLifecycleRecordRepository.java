package eu.bbmri_eric.negotiator.database.repository;

import eu.bbmri_eric.negotiator.configuration.state_machine.resource.NegotiationResourceState;
import eu.bbmri_eric.negotiator.database.model.Negotiation;
import eu.bbmri_eric.negotiator.database.model.NegotiationResourceLifecycleRecord;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface NegotiationResourceLifecycleRecordRepository
    extends JpaRepository<Negotiation, String>,
        JpaSpecificationExecutor<NegotiationResourceLifecycleRecord> {

  @Query(
      "SELECT nrlr.changedTo "
          + "FROM NegotiationResourceLifecycleRecord nrlr "
          + "WHERE nrlr.negotiation.id = :negotiationId AND nrlr.resource.sourceId = :resourceId "
          + "ORDER BY nrlr.creationDate DESC LIMIT 1")
  Optional<NegotiationResourceState> findNegotiationResourceStateById(
      String negotiationId, String resourceId);
}
