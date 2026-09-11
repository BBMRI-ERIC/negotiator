package eu.bbmri_eric.negotiator.lifecycle.definition;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
interface StateRepository extends JpaRepository<State, Long> {

  /**
   * Every State of one Definition Version. Ordering by id keeps refusal messages that print the
   * State set stable across runs.
   */
  List<State> findByLifecycleDefinitionIdOrderById(long definitionVersionId);
}
