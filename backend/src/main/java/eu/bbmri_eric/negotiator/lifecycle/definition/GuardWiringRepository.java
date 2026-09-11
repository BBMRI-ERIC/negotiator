package eu.bbmri_eric.negotiator.lifecycle.definition;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
interface GuardWiringRepository extends JpaRepository<GuardWiring, Long> {

  /**
   * Every Guard Wiring row of one Definition Version — both scopes, in one query, because the two
   * are distinguished by a nullable column rather than by a table and {@link DefinitionCompiler}
   * wants them together to fold one chain per Transition.
   *
   * <p>The join is a <em>left</em> one precisely because of that column: a definition-wide Guard
   * has no Transition, and an inner join would silently drop exactly the rows that apply to every
   * edge. Ordering is by id; the compiler sorts each scope by its own {@code sort_order} itself.
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
