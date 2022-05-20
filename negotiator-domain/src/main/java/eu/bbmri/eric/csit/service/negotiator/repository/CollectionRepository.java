package eu.bbmri.eric.csit.service.negotiator.repository;

import eu.bbmri.eric.csit.service.negotiator.model.Collection;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CollectionRepository extends JpaRepository<Collection, Long> {

  //  @Query("SELECT c FROM collection c JOIN FETCH p.biobank WHERE c.name = (:name)")
  Optional<Collection> findByName(String name);

  Optional<Collection> findBySourceId(String sourceId);

  //  @Query("SELECT c FROM Collection c JOIN FETCH c.biobank b WHERE c.id in :sourceId and b.source_id = :biobankSourceId")
  Set<Collection> findBySourceIdInAndBiobankSourceId(Set<String> sourceId, String biobankSourceId);
}
