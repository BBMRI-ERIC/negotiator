package eu.bbmri.eric.csit.service.negotiator.service;

import eu.bbmri.eric.csit.service.negotiator.database.model.AccessCriteriaSet;
import eu.bbmri.eric.csit.service.negotiator.database.repository.AccessCriteriaSetRepository;
import eu.bbmri.eric.csit.service.negotiator.exceptions.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccessCriteriaSetService {

  @Autowired private AccessCriteriaSetRepository accessCriteriaSetRepository;

  @Transactional
  public AccessCriteriaSet findByResourceEntityId(String resourceEntityId) {
    AccessCriteriaSet f = accessCriteriaSetRepository.findByResourceEntityId(resourceEntityId);
    if (f == null) {
      throw new EntityNotFoundException(resourceEntityId);
    }
    return f;
  }
}
