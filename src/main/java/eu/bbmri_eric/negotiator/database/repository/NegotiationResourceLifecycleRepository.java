package eu.bbmri_eric.negotiator.database.repository;

import eu.bbmri_eric.negotiator.configuration.state_machine.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.configuration.state_machine.resource.NegotiationResourceState;
import eu.bbmri_eric.negotiator.database.model.NegotiationResourceLifecycleRecord;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NegotiationResourceLifecycleRepository
    extends JpaRepository<NegotiationResourceLifecycleRecord, Long> {

  List<NegotiationResourceLifecycleRecord>
      findAllByResource_IdAndChangedToAndNegotiation_CurrentState(
          Long resourceId, NegotiationResourceState changedTo, NegotiationState currentState);
}
