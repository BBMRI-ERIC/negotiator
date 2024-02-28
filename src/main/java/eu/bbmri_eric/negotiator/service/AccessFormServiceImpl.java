package eu.bbmri_eric.negotiator.service;

import eu.bbmri_eric.negotiator.database.model.AccessForm;
import eu.bbmri_eric.negotiator.database.model.AccessFormElement;
import eu.bbmri_eric.negotiator.database.model.AccessFormSection;
import eu.bbmri_eric.negotiator.database.model.Request;
import eu.bbmri_eric.negotiator.database.model.Resource;
import eu.bbmri_eric.negotiator.database.repository.AccessFormRepository;
import eu.bbmri_eric.negotiator.database.repository.RequestRepository;
import eu.bbmri_eric.negotiator.dto.access_criteria.AccessCriteriaSetDTO;
import eu.bbmri_eric.negotiator.exceptions.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

@Service
public class AccessFormServiceImpl implements AccessFormService {
  RequestRepository requestRepository;

  AccessFormRepository accessFormRepository;
  ModelMapper modelMapper;

  public AccessFormServiceImpl(RequestRepository requestRepository, ModelMapper modelMapper) {
    this.requestRepository = requestRepository;
    this.modelMapper = modelMapper;
  }

  @Override
  @Transactional
  public AccessCriteriaSetDTO getAccessFormForRequest(String requestId)
      throws EntityNotFoundException {
    verifyArguments(requestId);
    Request request = findRequest(requestId);
    AccessForm accessForm = request.getResources().iterator().next().getAccessForm();
    AccessForm finalAccessForm = accessForm;
    if (request.getResources().stream()
        .allMatch(
            resource -> resource.getAccessForm().getName().equals(finalAccessForm.getName()))) {
      return modelMapper.map(accessForm, AccessCriteriaSetDTO.class);
    }
    accessForm = new AccessForm("Combined access form");
    for (Resource resource : request.getResources()) {
      for (AccessFormSection accessFormSection : resource.getAccessForm().getSections()) {
        if (!accessForm.getSections().contains(accessFormSection)) {
          accessForm.addSection(accessFormSection, accessForm.getSections().size());
        }
        for (AccessFormElement accessFormElement : accessFormSection.getAccessFormElements()) {
          AccessFormSection matchedSection =
              accessForm.getSections().stream()
                  .filter(sec -> sec.equals(accessFormSection))
                  .findFirst()
                  .get();
          if (!matchedSection.getAccessFormElements().contains(accessFormElement)) {
            accessForm.linkElementToSection(
                accessFormSection,
                accessFormElement,
                matchedSection.getAccessFormElements().size(),
                false);
          }
        }
      }
    }
    return modelMapper.map(accessForm, AccessCriteriaSetDTO.class);
  }

  private AccessCriteriaSetDTO getCombinedAccessForm(Request request, AccessForm accessForm) {
    AccessForm combinedAccessForm = combineAccessForms(request, accessForm);
    return modelMapper.map(combinedAccessForm, AccessCriteriaSetDTO.class);
  }

  private static AccessForm combineAccessForms(Request request, AccessForm accessForm) {
    for (Resource resource : request.getResources()) {
      if (!resource.getAccessForm().equals(accessForm)) {
        accessForm.getSections().addAll(resource.getAccessForm().getSections());
      }
    }
    return accessForm;
  }

  private static void verifyArguments(String requestId) {
    if (requestId == null) {
      throw new IllegalArgumentException("Request id cannot be null");
    }
  }

  private Request findRequest(String requestId) {
    return requestRepository
        .findById(requestId)
        .orElseThrow(() -> new EntityNotFoundException(requestId));
  }
}
