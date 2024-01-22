package eu.bbmri.eric.csit.service.negotiator.database.repository;

import eu.bbmri.eric.csit.service.negotiator.configuration.state_machine.resource.NegotiationResourceState;
import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationResourceLifecycleRecord;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface NegotiationResourceLifecycleRecordRepository
    extends JpaRepository<NegotiationResourceLifecycleRecord, Long>,
        JpaSpecificationExecutor<NegotiationResourceLifecycleRecord> {

  List<NegotiationResourceLifecycleRecord> findByChangedTo(NegotiationResourceState state);
}
