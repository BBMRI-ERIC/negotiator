package eu.bbmri_eric.negotiator.database.repository;

import eu.bbmri_eric.negotiator.database.model.AccessCriteriaSection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccessCriteriaSectionRepository
    extends JpaRepository<AccessCriteriaSection, Long> {}
