package eu.bbmri_eric.negotiator.lifecycle.definition;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
interface ActionWiringRepository extends JpaRepository<ActionWiring, Long> {

  /**
   * Every Action Wiring row of one Definition Version, reached through the Transition each one
   * hangs off.
   *
   * <p>The only one of the five loader queries that cannot name {@code lifecycle_definition_id}:
   * {@code action_wiring} carries no definition column, since Actions are always transition-scoped
   * and the Transition already implies the version. That is ADR 0002's shape, and the join is what
   * it costs to read by version. The alias on the fetch join is what keeps it to one join rather
   * than two.
   */
  @Query(
      """
      select aw from ActionWiring aw
        join fetch aw.transition t
      where t.lifecycleDefinition.id = :definitionVersionId
      order by aw.id
      """)
  List<ActionWiring> findByTransitionLifecycleDefinitionIdOrderById(long definitionVersionId);
}
