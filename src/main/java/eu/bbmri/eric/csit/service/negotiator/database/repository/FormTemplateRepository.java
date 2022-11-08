package eu.bbmri.eric.csit.service.negotiator.database.repository;

import eu.bbmri.eric.csit.service.negotiator.database.model.FormTemplate;
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.transaction.Transactional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
@Transactional
public interface FormTemplateRepository extends JpaRepository<FormTemplate, Long> {

  @EntityGraph(value = "template-with-details")
  FormTemplate findByResourceId(Long resourceId);

}
