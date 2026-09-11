package eu.bbmri_eric.negotiator.lifecycle.definition;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
interface TransitionRepository extends JpaRepository<Transition, Long> {

  /**
   * Every Transition of one Definition Version, with the three vertices each edge names already
   * initialized.
   *
   * <p>Written out rather than derived, for the fetch joins alone. {@link DefinitionCompiler} reads
   * the name off all three of a Transition's references, and it does so after the loader's
   * transaction has committed — so a lazy association here is not a query this method avoided, it
   * is a failure on a detached row. The joins are inner because all three columns are {@code NOT
   * NULL}.
   */
  @Query(
      """
      select t from Transition t
        join fetch t.fromState
        join fetch t.toState
        join fetch t.event
      where t.lifecycleDefinition.id = :definitionVersionId
      order by t.id
      """)
  List<Transition> findByLifecycleDefinitionIdOrderById(long definitionVersionId);
}
