package eu.bbmri_eric.negotiator.form;

import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.form.dto.AccessFormCreateDTO;
import eu.bbmri_eric.negotiator.form.dto.AccessFormDTO;
import eu.bbmri_eric.negotiator.form.dto.ElementLinkDTO;
import eu.bbmri_eric.negotiator.form.dto.SectionLinkDTO;
import eu.bbmri_eric.negotiator.governance.resource.Resource;
import eu.bbmri_eric.negotiator.negotiation.Request;
import eu.bbmri_eric.negotiator.negotiation.RequestRepository;
import jakarta.transaction.Transactional;
import lombok.NonNull;
import lombok.extern.apachecommons.CommonsLog;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@CommonsLog
public class AccessFormServiceImpl implements AccessFormService {
  private final AccessFormSectionRepository accessFormSectionRepository;
  private final AccessFormElementRepository accessFormElementRepository;
  RequestRepository requestRepository;
  AccessFormRepository accessFormRepository;
  ModelMapper modelMapper;

  public AccessFormServiceImpl(
      RequestRepository requestRepository,
      AccessFormRepository accessFormRepository,
      ModelMapper modelMapper,
      AccessFormSectionRepository accessFormSectionRepository,
      AccessFormElementRepository accessFormElementRepository) {
    this.requestRepository = requestRepository;
    this.accessFormRepository = accessFormRepository;
    this.modelMapper = modelMapper;
    this.accessFormSectionRepository = accessFormSectionRepository;
    this.accessFormElementRepository = accessFormElementRepository;
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
          accessForm.linkElementToSection(
              matchedSection,
              accessFormElement,
              matchedSection.getAccessFormElements().size(),
              accessFormElement.isRequired());
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

  @Override
  @Transactional
  public Iterable<AccessFormDTO> getAllAccessForms(@NonNull Pageable pageable) {
    return accessFormRepository
        .findAll(pageable)
        .map(accessForm -> modelMapper.map(accessForm, AccessFormDTO.class));
  }

  @Override
  public AccessFormDTO createAccessForm(AccessFormCreateDTO createDTO) {
    AccessForm accessForm = modelMapper.map(createDTO, AccessForm.class);
    return modelMapper.map(accessFormRepository.save(accessForm), AccessFormDTO.class);
  }

  @Override
  @Transactional
  public AccessFormDTO addSection(SectionLinkDTO linkDTO, Long formId) {
    AccessForm accessForm =
        accessFormRepository
            .findById(formId)
            .orElseThrow(() -> new EntityNotFoundException(formId));
    AccessFormSection sectionToBeLinked =
        accessFormSectionRepository
            .findById(linkDTO.getSectionId())
            .orElseThrow(() -> new EntityNotFoundException(linkDTO.getSectionId()));
    accessForm.linkSection(sectionToBeLinked, linkDTO.getSectionOrder());
    return modelMapper.map(accessForm, AccessFormDTO.class);
  }

  @Override
  @Transactional
  public AccessFormDTO removeSection(Long formId, Long sectionId) {
    AccessForm accessForm =
        accessFormRepository
            .findById(formId)
            .orElseThrow(() -> new EntityNotFoundException(formId));
    AccessFormSection sectionToBeRemoved =
        accessFormSectionRepository
            .findById(sectionId)
            .orElseThrow(() -> new EntityNotFoundException(sectionId));
    accessForm.unlinkSection(sectionToBeRemoved);
    return modelMapper.map(accessFormRepository.save(accessForm), AccessFormDTO.class);
  }

  @Override
  @Transactional
  public AccessFormDTO addElement(ElementLinkDTO linkDTO, Long formId, Long sectionId) {
    AccessForm accessForm =
        accessFormRepository
            .findById(formId)
            .orElseThrow(() -> new EntityNotFoundException(formId));
    AccessFormElement elementToBeLinked =
        accessFormElementRepository
            .findById(linkDTO.getElementId())
            .orElseThrow(() -> new EntityNotFoundException(linkDTO.getElementId()));
    AccessFormSection accessFormSection =
        accessForm.getLinkedSections().stream()
            .filter(section -> section.getId().equals(sectionId))
            .findFirst()
            .orElseThrow(() -> new EntityNotFoundException(sectionId));
    accessForm.linkElementToSection(
        accessFormSection, elementToBeLinked, linkDTO.getElementOrder(), linkDTO.isRequired());
    return modelMapper.map(accessForm, AccessFormDTO.class);
  }

  @Override
  @Transactional
  public AccessFormDTO removeElement(Long formId, Long sectionId, Long elementId) {
    AccessForm accessForm =
        accessFormRepository
            .findById(formId)
            .orElseThrow(() -> new EntityNotFoundException(formId));
    AccessFormElement elementToBeLinked =
        accessFormElementRepository
            .findById(elementId)
            .orElseThrow(() -> new EntityNotFoundException(elementId));
    AccessFormSection accessFormSection =
        accessForm.getLinkedSections().stream()
            .filter(section -> section.getId().equals(sectionId))
            .findFirst()
            .orElseThrow(() -> new EntityNotFoundException(sectionId));
    accessForm.unlinkElementFromSection(accessFormSection, elementToBeLinked);
    return modelMapper.map(accessFormRepository.save(accessForm), AccessFormDTO.class);
  }

  private static boolean allResourcesHaveTheSameForm(Request request, AccessForm finalAccessForm) {
    return request.getResources().stream()
        .allMatch(resource -> resource.getAccessForm().getName().equals(finalAccessForm.getName()));
  }

  private static boolean formDoesntContainSection(
      AccessFormSection accessFormSection, AccessForm accessForm) {
    return accessForm.getLinkedSections().stream()
        .noneMatch(section -> section.equals(accessFormSection));
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
