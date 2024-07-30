package eu.bbmri_eric.negotiator.database.repository;

import eu.bbmri_eric.negotiator.database.model.InformationSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InformationSubmissionRepository
    extends JpaRepository<InformationSubmission, Long> {}
