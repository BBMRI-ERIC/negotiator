package eu.bbmri.eric.csit.service.negotiator.repository;

import eu.bbmri.eric.csit.service.negotiator.model.Resource;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public interface ResourceRepository extends JpaRepository<Resource, Long> {

  //  @Query("SELECT c FROM collection c JOIN FETCH p.biobank WHERE c.name = (:name)")
  Optional<Resource> findByName(String name);

  Optional<Resource> findBySourceId(String sourceId);
//
//  @Query(
//      "SELECT c FROM Collection c JOIN FETCH c.biobank b WHERE c.sourceId in :sourceId and b.sourceId = :biobankSourceId")
//  Set<Collection> findBySourceIdInAndBiobankSourceId(Set<String> sourceId, String biobankSourceId);
}
