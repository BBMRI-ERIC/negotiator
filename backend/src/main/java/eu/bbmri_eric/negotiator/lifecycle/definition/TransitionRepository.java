package eu.bbmri_eric.negotiator.lifecycle.definition;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
interface TransitionRepository extends JpaRepository<Transition, Long> {

  /**
   * Every Transition of one Definition Version, with its two States and its Event initialized.
   *
   * <p>The query exists for the fetch joins. {@link DefinitionCompiler} reads all three after the
   * loader's transaction commits, so a lazy association would fail on a detached row. The joins are
   * inner because all three columns are {@code NOT NULL}.
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
