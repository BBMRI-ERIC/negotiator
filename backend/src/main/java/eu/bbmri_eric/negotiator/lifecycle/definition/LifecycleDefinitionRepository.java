package eu.bbmri_eric.negotiator.lifecycle.definition;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** No finder by {@code (familyKey, version)}: the row id is the sole machine identity. */
@Repository
interface LifecycleDefinitionRepository extends JpaRepository<LifecycleDefinition, Long> {}
