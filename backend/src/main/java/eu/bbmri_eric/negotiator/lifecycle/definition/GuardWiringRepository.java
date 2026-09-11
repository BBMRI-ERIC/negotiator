package eu.bbmri_eric.negotiator.lifecycle.definition;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
interface GuardWiringRepository extends JpaRepository<GuardWiring, Long> {

  /**
   * Every Guard Wiring row of one Definition Version, both scopes in one query, ordered by id. The
   * compiler sorts each scope by {@code sort_order}.
   *
   * <p>The join is a left join because a definition-wide Guard has no Transition, and an inner join
   * would drop those rows.
   */
  @Query(
      """
      select gw from GuardWiring gw
        left join fetch gw.transition
      where gw.lifecycleDefinition.id = :definitionVersionId
      order by gw.id
      """)
  List<GuardWiring> findByLifecycleDefinitionIdOrderById(long definitionVersionId);
}
