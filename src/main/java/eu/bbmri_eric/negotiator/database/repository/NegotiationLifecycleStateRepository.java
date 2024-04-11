package eu.bbmri_eric.negotiator.database.repository;

import eu.bbmri_eric.negotiator.database.model.NegotiationStateMetadata;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NegotiationLifecycleStateRepository
    extends JpaRepository<NegotiationStateMetadata, Long> {}
