package eu.bbmri_eric.negotiator.lifecycle.definition;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** No finder by {@code (familyKey, version)}: the row id is the sole machine identity. */
@Repository
interface LifecycleDefinitionRepository extends JpaRepository<LifecycleDefinition, Long> {

  /**
   * A list, not an {@link Optional}: only one version <em>per family</em> can be active, so several
   * families of one scope having an active version is a state the schema permits and Definition
   * Resolution has to be able to report.
   */
  List<LifecycleDefinition> findByScopeAndActiveTrue(DefinitionScope scope);

  /**
   * An {@link Optional} is safe here where it is not above: the partial unique index on {@code
   * is_global_default} allows one active global default in the whole table.
   */
  Optional<LifecycleDefinition> findByScopeAndActiveTrueAndGlobalDefaultTrue(DefinitionScope scope);
}
