package eu.bbmri_eric.negotiator.database.repository;

import eu.bbmri_eric.negotiator.database.model.NegotiationEventMetadata;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NegotiationLifecycleEventRepository
    extends JpaRepository<NegotiationEventMetadata, Long> {}
