package eu.bbmri_eric.negotiator.info_requirement;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InformationRequirementRepository
    extends JpaRepository<InformationRequirement, Long> {

  boolean existsByForEvent(String event);
}
