package eu.bbmri_eric.negotiator.form.service;

import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.form.AccessForm;
import eu.bbmri_eric.negotiator.form.AccessFormElement;
import eu.bbmri_eric.negotiator.form.AccessFormSection;
import eu.bbmri_eric.negotiator.form.dto.AccessFormCreateDTO;
import eu.bbmri_eric.negotiator.form.dto.AccessFormDTO;
import eu.bbmri_eric.negotiator.form.dto.AccessFormUpdateDTO;
import eu.bbmri_eric.negotiator.form.dto.AccessFormUpdateElementDTO;
import eu.bbmri_eric.negotiator.form.dto.AccessFormUpdateSectionDTO;
import eu.bbmri_eric.negotiator.form.dto.ElementLinkDTO;
import eu.bbmri_eric.negotiator.form.dto.SectionLinkDTO;
import eu.bbmri_eric.negotiator.form.repository.AccessFormElementRepository;
import eu.bbmri_eric.negotiator.form.repository.AccessFormRepository;
import eu.bbmri_eric.negotiator.form.repository.AccessFormSectionRepository;
import eu.bbmri_eric.negotiator.governance.resource.Resource;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.negotiation.request.Request;
import eu.bbmri_eric.negotiator.negotiation.request.RequestRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Set;
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
  private final RequestRepository requestRepository;
  private final NegotiationRepository negotiationRepository;
  private final AccessFormRepository accessFormRepository;
  private final ModelMapper modelMapper;

  public AccessFormServiceImpl(
      RequestRepository requestRepository,
      AccessFormRepository accessFormRepository,
      ModelMapper modelMapper,
      AccessFormSectionRepository accessFormSectionRepository,
      AccessFormElementRepository accessFormElementRepository,
      NegotiationRepository negotiationRepository) {
    this.requestRepository = requestRepository;
    this.accessFormRepository = accessFormRepository;
    this.modelMapper = modelMapper;
    this.accessFormSectionRepository = accessFormSectionRepository;
    this.accessFormElementRepository = accessFormElementRepository;
    this.negotiationRepository = negotiationRepository;
  }

  @Override
  @Transactional
  public AccessFormDTO getAccessFormForRequest(String requestId) throws EntityNotFoundException {
    Request request = findRequest(requestId);
    return buildAccessForm(request.getResources());
  }

  private AccessFormDTO buildAccessForm(Set<Resource> resources) {
    AccessForm accessForm = resources.iterator().next().getAccessForm();
    AccessForm finalAccessForm = accessForm;
    if (allResourcesHaveTheSameForm(resources, finalAccessForm)) {
      return modelMapper.map(accessForm, AccessFormDTO.class);
    }
    accessForm = new AccessForm("Combined access form");
    int counter = 0;
    for (Resource resource : resources) {
      for (AccessFormSection accessFormSection : resource.getAccessForm().getLinkedSections()) {
        if (formDoesntContainSection(accessForm.getId(), accessFormSection.getId())) {
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
  public AccessFormDTO getAccessFormForNegotiation(String negotiationId) {
    Negotiation negotiation =
        negotiationRepository
            .findById(negotiationId)
            .orElseThrow(() -> new EntityNotFoundException(negotiationId));
    return buildAccessForm(negotiation.getResources());
  }

  @Override
  @Transactional
  public AccessFormDTO getAccessForm(Long id) {
    AccessForm form =
        accessFormRepository.findById(id).orElseThrow(() -> new EntityNotFoundException(id));
    return modelMapper.map(form, AccessFormDTO.class);
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
  public AccessFormDTO updateAccessForm(Long formId, AccessFormUpdateDTO formUpdateDTO) {
    AccessForm accessForm =
        accessFormRepository
            .findById(formId)
            .orElseThrow(() -> new EntityNotFoundException(formId));
    accessForm = resetForm(accessForm);
    accessForm.setName(formUpdateDTO.getName());
    List<AccessFormUpdateSectionDTO> sectionsDTO = formUpdateDTO.getSections();
    for (int i = 0; i < sectionsDTO.size(); i++) {
      AccessFormUpdateSectionDTO sectionDTO = sectionsDTO.get(i);
      AccessFormSection section =
          accessFormSectionRepository
              .findById(sectionDTO.getId())
              .orElseThrow(() -> new EntityNotFoundException(sectionDTO.getId()));
      accessForm.linkSection(section, i);

      List<AccessFormUpdateElementDTO> elementsDTO = sectionDTO.getElements();
      for (int j = 0; j < elementsDTO.size(); j++) {
        AccessFormUpdateElementDTO elementDTO = elementsDTO.get(j);
        AccessFormElement element =
            accessFormElementRepository
                .findById(elementDTO.getId())
                .orElseThrow(() -> new EntityNotFoundException(elementDTO.getId()));
        accessForm.linkElementToSection(section, element, j, elementDTO.getRequired());
      }
    }
    return modelMapper.map(accessFormRepository.saveAndFlush(accessForm), AccessFormDTO.class);
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
    return modelMapper.map(removeSection(accessForm, sectionToBeRemoved), AccessFormDTO.class);
  }

  private AccessForm removeSection(AccessForm accessForm, AccessFormSection section) {
    accessForm.unlinkSection(section);
    return accessForm;
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
    return modelMapper.map(
        removeElement(accessForm, accessFormSection, elementToBeLinked), AccessFormDTO.class);
  }

  private AccessForm removeElement(
      AccessForm accessForm, AccessFormSection section, AccessFormElement element) {
    accessForm.unlinkElementFromSection(section, element);
    return accessForm;
  }

  private static boolean allResourcesHaveTheSameForm(
      Set<Resource> resources, AccessForm finalAccessForm) {
    return resources.stream()
        .allMatch(resource -> resource.getAccessForm().getName().equals(finalAccessForm.getName()));
  }

  private boolean formDoesntContainSection(Long accessFormId, Long accessFormSectionId) {
    return !accessFormRepository.isSectionPartOfAccessForm(accessFormId, accessFormSectionId);
  }

  private static void verifyArguments(String requestId) {
    if (requestId == null) {
      throw new IllegalArgumentException("Request id cannot be null");
    }
  }

  private Request findRequest(String requestId) {
    verifyArguments(requestId);
    return requestRepository
        .findById(requestId)
        .orElseThrow(() -> new EntityNotFoundException(requestId));
  }

  private AccessForm resetForm(AccessForm accessForm) {
    List<AccessFormSection> sections = List.copyOf(accessForm.getLinkedSections());
    for (AccessFormSection section : sections) {
      List<AccessFormElement> elements = List.copyOf(section.getAccessFormElements());
      for (AccessFormElement element : elements) {
        removeElement(accessForm, section, element);
      }
      removeSection(accessForm, section);
    }
    return accessFormRepository.saveAndFlush(accessForm);
  }
}
