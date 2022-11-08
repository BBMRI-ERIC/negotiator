package eu.bbmri.eric.csit.service.negotiator.service;

import eu.bbmri.eric.csit.service.negotiator.database.model.FormTemplate;
import eu.bbmri.eric.csit.service.negotiator.database.repository.FormTemplateRepository;
import eu.bbmri.eric.csit.service.negotiator.exceptions.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FormTemplateService {

  @Autowired private FormTemplateRepository formRepository;

  @Transactional
  public FormTemplate findByResourceId(Long resourceId) {
    return formRepository.findByResourceId(resourceId);
  }

  @Transactional
  public FormTemplate findByResourceEntityId(String resourceEntityId) {
    FormTemplate f = formRepository.findByResourceEntityId(resourceEntityId);
    if (f == null) {
      throw new EntityNotFoundException(resourceEntityId);
    }
    return f;
  }
}
