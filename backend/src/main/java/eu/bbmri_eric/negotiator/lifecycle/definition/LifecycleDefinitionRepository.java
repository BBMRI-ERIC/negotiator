package eu.bbmri_eric.negotiator.lifecycle.definition;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for Definition Versions, the root of the Lifecycle Definition schema.
 *
 * <p>There is deliberately no finder taking a {@code familyKey} and a {@code version}: by ADR 0003
 * the row id is the sole machine identity and the version integer carries none, so a lookup by that
 * pair would hand callers an identity the model does not have.
 */
@Repository
interface LifecycleDefinitionRepository extends JpaRepository<LifecycleDefinition, Long> {}
