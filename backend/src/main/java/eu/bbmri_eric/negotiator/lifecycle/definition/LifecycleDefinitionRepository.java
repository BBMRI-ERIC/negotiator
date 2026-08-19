package eu.bbmri_eric.negotiator.lifecycle.definition;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** No finder by {@code (familyKey, version)}, because the row id is the only identity. */
@Repository
interface LifecycleDefinitionRepository extends JpaRepository<LifecycleDefinition, Long> {}
