package eu.bbmri_eric.negotiator.database.repository;

import eu.bbmri_eric.negotiator.configuration.state_machine.resource.NegotiationResourceEvent;
import eu.bbmri_eric.negotiator.database.model.InformationRequirement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InformationRequirementRepository
    extends JpaRepository<InformationRequirement, Long> {

  boolean existsByForEvent(NegotiationResourceEvent event);
}
