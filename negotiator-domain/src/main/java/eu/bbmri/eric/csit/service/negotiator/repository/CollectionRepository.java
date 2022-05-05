package eu.bbmri.eric.csit.service.negotiator.repository;

import eu.bbmri.eric.csit.service.negotiator.model.Collection;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CollectionRepository extends JpaRepository<Collection, Long> {

  //  @Query("SELECT c FROM collection c JOIN FETCH p.biobank WHERE c.name = (:name)")
  Optional<Collection> findByName(String name);

  Optional<Collection> findBySourceId(String sourceId);

  //  Optional<Collection> findBySourceIdAndBiobankId(String sourceId, String biobankSourceId);

}
