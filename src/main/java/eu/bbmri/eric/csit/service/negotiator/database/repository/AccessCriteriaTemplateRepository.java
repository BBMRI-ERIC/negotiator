package eu.bbmri.eric.csit.service.negotiator.database.repository;

import eu.bbmri.eric.csit.service.negotiator.database.model.AccessCriteriaTemplate;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AccessCriteriaTemplateRepository extends JpaRepository<AccessCriteriaTemplate, Long> {

  @EntityGraph(value = "template-with-details")
  AccessCriteriaTemplate findByResourceId(Long resourceId);

  @Query(
      value =
          "SELECT DISTINCT a "
              + "FROM AccessCriteriaTemplate a "
              + "JOIN FETCH a.resource r "
              + "WHERE r.sourceId = :entityId")
  AccessCriteriaTemplate findByResourceEntityId(String entityId);
}
