package eu.bbmri.eric.csit.service.negotiator.repository;

import eu.bbmri.eric.csit.service.negotiator.model.Collection;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public interface CollectionRepository extends JpaRepository<Collection, Long> {

  //  @Query("SELECT c FROM collection c JOIN FETCH p.biobank WHERE c.name = (:name)")
  Optional<Collection> findByName(String name);

  Optional<Collection> findBySourceId(String sourceId);

  @Query(
      "SELECT c FROM Collection c JOIN FETCH c.biobank b WHERE c.sourceId in :sourceId and b.sourceId = :biobankSourceId")
  Set<Collection> findBySourceIdInAndBiobankSourceId(Set<String> sourceId, String biobankSourceId);
}
