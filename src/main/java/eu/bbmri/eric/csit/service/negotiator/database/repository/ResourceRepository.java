package eu.bbmri.eric.csit.service.negotiator.database.repository;

import eu.bbmri.eric.csit.service.negotiator.database.model.Resource;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public interface ResourceRepository extends JpaRepository<Resource, Long> {

  //  @Request("SELECT c FROM collection c JOIN FETCH p.biobank WHERE c.name = (:name)")
  Optional<Resource> findByName(String name);

  Optional<Resource> findBySourceId(String sourceId);

  @Query(
      "SELECT r FROM Resource r JOIN FETCH r.parent p WHERE r.sourceId in :sourceId and p.sourceId = :parentId")
  Set<Resource> findBySourceIdInAndParentSourceId(Set<String> sourceId, String parentId);
}
