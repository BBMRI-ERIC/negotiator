package eu.bbmri_eric.negotiator.lifecycle.definition;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
interface ActionWiringRepository extends JpaRepository<ActionWiring, Long> {

  /**
   * Every Action Wiring row of one Definition Version, reached through its Transition, because
   * {@code action_wiring} has no definition column. The alias on the fetch join keeps it to one
   * join rather than two.
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
