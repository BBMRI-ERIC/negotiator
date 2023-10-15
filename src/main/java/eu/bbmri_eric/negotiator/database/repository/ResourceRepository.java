package eu.bbmri_eric.negotiator.database.repository;

import eu.bbmri_eric.negotiator.database.model.Resource;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public interface ResourceRepository extends JpaRepository<Resource, Long> {

  Optional<Resource> findByName(String name);

  Optional<Resource> findBySourceId(String sourceId);
}
