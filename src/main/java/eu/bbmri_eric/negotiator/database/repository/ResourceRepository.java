package eu.bbmri_eric.negotiator.database.repository;

import eu.bbmri_eric.negotiator.database.model.Resource;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResourceRepository extends JpaRepository<Resource, Long> {

  Optional<Resource> findByName(String name);

  Optional<Resource> findBySourceId(String sourceId);

  List<Resource> findAllBySourceIdIn(Set<String> sourceIds);
}
