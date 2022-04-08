package eu.bbmri.eric.csit.service.repository;

import eu.bbmri.eric.csit.service.model.Collection;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CollectionRepository extends JpaRepository<Collection, Long> {

  //  @Query("SELECT c FROM collection c JOIN FETCH p.biobank WHERE c.name = (:name)")
  Optional<Collection> findByName(String name);

  Optional<Collection> findBySourceId(String sourceId);

}
