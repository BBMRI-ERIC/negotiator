package eu.bbmri.eric.csit.service.negotiator.database.repository;

import eu.bbmri.eric.csit.service.negotiator.database.model.AccessCriteriaSet;
import java.util.Set;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AccessCriteriaSetRepository extends JpaRepository<AccessCriteriaSet, Long> {
  @Query(
      value =
          "SELECT DISTINCT a "
              + "FROM AccessCriteriaSet a "
              + "JOIN a.resources r "
              + "JOIN FETCH a.accessCriteriaSetLink c "
              + "JOIN FETCH c.accessCriteria ac "
              + "WHERE r.sourceId = :entityId")
  AccessCriteriaSet findByResourceEntityId(String entityId);
}
