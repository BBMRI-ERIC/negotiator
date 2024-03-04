package eu.bbmri_eric.negotiator.service;

import eu.bbmri_eric.negotiator.database.model.AccessForm;
import eu.bbmri_eric.negotiator.database.model.AccessFormElement;
import eu.bbmri_eric.negotiator.database.model.AccessFormSection;
import eu.bbmri_eric.negotiator.database.model.Request;
import eu.bbmri_eric.negotiator.database.model.Resource;
import eu.bbmri_eric.negotiator.database.repository.AccessFormRepository;
import eu.bbmri_eric.negotiator.database.repository.RequestRepository;
import eu.bbmri_eric.negotiator.dto.access_form.AccessFormDTO;
import eu.bbmri_eric.negotiator.exceptions.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.extern.apachecommons.CommonsLog;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

@Service
@CommonsLog
public class AccessFormServiceImpl implements AccessFormService {
  RequestRepository requestRepository;
  AccessFormRepository accessFormRepository;
  ModelMapper modelMapper;

  public AccessFormServiceImpl(
      RequestRepository requestRepository,
      AccessFormRepository accessFormRepository,
      ModelMapper modelMapper) {
    this.requestRepository = requestRepository;
    this.accessFormRepository = accessFormRepository;
    this.modelMapper = modelMapper;
  }

  @Override
  @Transactional
  public AccessFormDTO getAccessFormForRequest(String requestId) throws EntityNotFoundException {
    verifyArguments(requestId);
    Request request = findRequest(requestId);
    AccessForm accessForm = request.getResources().iterator().next().getAccessForm();
    AccessForm finalAccessForm = accessForm;
    if (allResourcesHaveTheSameForm(request, finalAccessForm)) {
      return modelMapper.map(accessForm, AccessFormDTO.class);
    }
    accessForm = new AccessForm("Combined access form");
    int counter = 0;
    for (Resource resource : request.getResources()) {
      for (AccessFormSection accessFormSection : resource.getAccessForm().getLinkedSections()) {
        if (formDoesntContainSection(accessFormSection, accessForm)) {
          accessForm.linkSection(accessFormSection, counter);
          counter++;
        }
        log.debug(resource.getAccessForm().getLinkedSections().size());
        for (AccessFormElement accessFormElement : accessFormSection.getAccessFormElements()) {
          AccessFormSection matchedSection =
              accessForm.getLinkedSections().stream()
                  .filter(sec -> sec.equals(accessFormSection))
                  .findFirst()
                  .get();
          if (sectionDoesntContainElement(accessFormElement, matchedSection)) {
            accessForm.linkElementToSection(
                matchedSection,
                accessFormElement,
                matchedSection.getAccessFormElements().size(),
                accessFormElement.isRequired());
          }
        }
      }
    }
    return modelMapper.map(accessForm, AccessFormDTO.class);
  }

  @Override
  @Transactional
  public AccessFormDTO getAccessForm(Long id) {
    return modelMapper.map(
        accessFormRepository.findById(id).orElseThrow(() -> new EntityNotFoundException(id)),
        AccessFormDTO.class);
  }

  private static boolean allResourcesHaveTheSameForm(Request request, AccessForm finalAccessForm) {
    return request.getResources().stream()
        .allMatch(resource -> resource.getAccessForm().getName().equals(finalAccessForm.getName()));
  }

  private static boolean sectionDoesntContainElement(
      AccessFormElement accessFormElement, AccessFormSection matchedSection) {
    return !matchedSection.getAccessFormElements().contains(accessFormElement);
  }

  private static boolean formDoesntContainSection(
      AccessFormSection accessFormSection, AccessForm accessForm) {
    return accessForm.getLinkedSections().stream()
        .noneMatch(section -> section.getName().equals(accessFormSection.getName()));
  }

  private AccessFormDTO getCombinedAccessForm(Request request, AccessForm accessForm) {
    AccessForm combinedAccessForm = combineAccessForms(request, accessForm);
    return modelMapper.map(combinedAccessForm, AccessFormDTO.class);
  }

  private static AccessForm combineAccessForms(Request request, AccessForm accessForm) {
    for (Resource resource : request.getResources()) {
      if (!resource.getAccessForm().equals(accessForm)) {
        accessForm.getLinkedSections().addAll(resource.getAccessForm().getLinkedSections());
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
