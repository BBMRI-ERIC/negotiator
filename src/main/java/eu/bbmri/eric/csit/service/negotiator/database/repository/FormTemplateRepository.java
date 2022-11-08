package eu.bbmri.eric.csit.service.negotiator.database.repository;

import eu.bbmri.eric.csit.service.negotiator.database.model.FormTemplate;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface FormTemplateRepository extends JpaRepository<FormTemplate, Long> {

  @EntityGraph(value = "template-with-details")
  FormTemplate findByResourceId(Long resourceId);

  @Query(
      value =
          "SELECT DISTINCT f "
              + "FROM FormTemplate f "
              + "JOIN FETCH f.resource r "
              + "WHERE r.sourceId = :entityId")
//  @EntityGraph(value = "template-with-details")
  FormTemplate findByResourceEntityId(String entityId);
}
