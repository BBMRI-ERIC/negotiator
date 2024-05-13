package eu.bbmri_eric.negotiator.database.repository;

import eu.bbmri_eric.negotiator.database.model.DirectorySyncJobRecord;
import eu.bbmri_eric.negotiator.database.model.DirectorySyncJobState;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface DirectorySyncJobRecordRepository
    extends JpaRepository<DirectorySyncJobRecord, String>,
        JpaSpecificationExecutor<DirectorySyncJobRecord> {

  Optional<DirectorySyncJobRecord> findDetailedById(String id);

  Optional<DirectorySyncJobRecord> findByJobState(DirectorySyncJobState jobState);
}
