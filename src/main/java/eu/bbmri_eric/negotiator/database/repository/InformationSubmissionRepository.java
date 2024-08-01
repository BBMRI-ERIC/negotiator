package eu.bbmri_eric.negotiator.database.repository;

import eu.bbmri_eric.negotiator.database.model.InformationSubmission;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InformationSubmissionRepository
    extends JpaRepository<InformationSubmission, Long> {
  boolean existsByResource_SourceIdAndNegotiation_Id(String sourceId, String negotiationId);

  Set<InformationSubmission> findAllByNegotiation_Id(String negotiationId);
}
