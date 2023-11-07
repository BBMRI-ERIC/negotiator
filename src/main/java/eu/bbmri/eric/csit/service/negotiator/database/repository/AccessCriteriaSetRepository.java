package eu.bbmri.eric.csit.service.negotiator.database.repository;

import eu.bbmri.eric.csit.service.negotiator.database.model.AccessCriteriaSet;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AccessCriteriaSetRepository extends JpaRepository<AccessCriteriaSet, Long> {

  Optional<AccessCriteriaSet> findById(Long id);

  @Query(
      value =
          "SELECT DISTINCT a "
              + "FROM AccessCriteriaSet a "
              + "JOIN FETCH a.sections s "
              + "JOIN FETCH s.accessCriteriaSectionLink acs "
              + "JOIN FETCH acs.accessCriteria ac "
              + "JOIN a.resources r "
              + "WHERE r.sourceId = :entityId")
  Optional<AccessCriteriaSet> findByResourceId(String entityId);
}
