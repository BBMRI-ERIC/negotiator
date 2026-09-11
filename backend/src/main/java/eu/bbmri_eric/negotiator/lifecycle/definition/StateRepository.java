package eu.bbmri_eric.negotiator.lifecycle.definition;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
interface StateRepository extends JpaRepository<State, Long> {

  /**
   * Every State of one Definition Version, for {@link DefinitionVersionLoader}. Ordered by id so
   * that a compiled graph's State set, and therefore every refusal message that prints it, reads
   * the same way on every run.
   */
  List<State> findByLifecycleDefinitionIdOrderById(long definitionVersionId);
}
