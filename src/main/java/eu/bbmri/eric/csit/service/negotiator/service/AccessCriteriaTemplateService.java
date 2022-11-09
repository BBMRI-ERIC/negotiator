package eu.bbmri.eric.csit.service.negotiator.service;

import eu.bbmri.eric.csit.service.negotiator.database.model.AccessCriteriaTemplate;
import eu.bbmri.eric.csit.service.negotiator.database.repository.AccessCriteriaTemplateRepository;
import eu.bbmri.eric.csit.service.negotiator.exceptions.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccessCriteriaTemplateService {

  @Autowired private AccessCriteriaTemplateRepository accessCriteriaTemplateRepository;

  @Transactional
  public AccessCriteriaTemplate findByResourceId(Long resourceId) {
    return accessCriteriaTemplateRepository.findByResourceId(resourceId);
  }

  @Transactional
  public AccessCriteriaTemplate findByResourceEntityId(String resourceEntityId) {
    AccessCriteriaTemplate f = accessCriteriaTemplateRepository.findByResourceEntityId(resourceEntityId);
    if (f == null) {
      throw new EntityNotFoundException(resourceEntityId);
    }
    return f;
  }
}
