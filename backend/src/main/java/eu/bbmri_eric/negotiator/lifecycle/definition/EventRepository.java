package eu.bbmri_eric.negotiator.lifecycle.definition;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
interface EventRepository extends JpaRepository<Event, Long> {

  /** Every Event of one Definition Version, ordered as {@link StateRepository}'s finder is. */
  List<Event> findByLifecycleDefinitionIdOrderById(long definitionVersionId);
}
