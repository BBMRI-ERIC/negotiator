package eu.bbmri_eric.negotiator.lifecycle.definition;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** No finder by {@code (familyKey, version)}, because the row id is the only identity. */
@Repository
interface LifecycleDefinitionRepository extends JpaRepository<LifecycleDefinition, Long> {

  /**
   * A list, because only one version per family can be active, and several families of one scope
   * may each have one. Definition Resolution reports that case.
   */
  List<LifecycleDefinition> findByScopeAndActiveTrue(DefinitionScope scope);

  /**
   * The partial unique index on {@code is_global_default} allows one active global default in the
   * whole table.
   */
  Optional<LifecycleDefinition> findByScopeAndActiveTrueAndGlobalDefaultTrue(DefinitionScope scope);
}
