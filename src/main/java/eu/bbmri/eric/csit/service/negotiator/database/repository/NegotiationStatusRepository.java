package eu.bbmri.eric.csit.service.negotiator.database.repository;

import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NegotiationStatusRepository extends JpaRepository<NegotiationStatus, String> {
    Optional<NegotiationStatus> findStatusByNegotiationId(String negotiationId);
}
