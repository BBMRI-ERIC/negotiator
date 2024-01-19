package eu.bbmri.eric.csit.service.negotiator.database.repository;

import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationResourceLifecycleRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface NegotiationResourceLifecycleRecordRepository
    extends JpaRepository<NegotiationResourceLifecycleRecord, Long>,
        JpaSpecificationExecutor<NegotiationResourceLifecycleRecord> {}
