package eu.bbmri_eric.negotiator.service;

import eu.bbmri_eric.negotiator.database.model.AccessForm;
import eu.bbmri_eric.negotiator.database.model.InformationRequirement;
import eu.bbmri_eric.negotiator.database.repository.AccessFormRepository;
import eu.bbmri_eric.negotiator.database.repository.InformationRequirementRepository;
import eu.bbmri_eric.negotiator.dto.InformationRequirementCreateDTO;
import eu.bbmri_eric.negotiator.dto.InformationRequirementDTO;
import eu.bbmri_eric.negotiator.exceptions.EntityNotFoundException;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Objects;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class InformationRequirementServiceImpl implements InformationRequirementService {
  private final InformationRequirementRepository requirementRepository;
  private final AccessFormRepository accessFormRepository;
  private final ModelMapper modelMapper;

  public InformationRequirementServiceImpl(
      InformationRequirementRepository requirementRepository,
      AccessFormRepository accessFormRepository,
      ModelMapper modelMapper) {
    this.requirementRepository = requirementRepository;
    this.accessFormRepository = accessFormRepository;
    this.modelMapper = modelMapper;
  }

  @Override
  public InformationRequirementDTO createInformationRequirement(
      InformationRequirementCreateDTO createDTO) {
    validateInformationRequirementCreateDTO(createDTO);
    AccessForm accessForm = findAccessFormById(createDTO.getRequiredAccessFormId());
    InformationRequirement requirement =
        new InformationRequirement(
            accessForm, createDTO.getForResourceEvent(), createDTO.isViewableOnlyByAdmin());
    return mapToDTO(requirementRepository.save(requirement));
  }

  @Override
  public InformationRequirementDTO updateInformationRequirement(
      InformationRequirementCreateDTO createDTO, Long id) {
    validateInformationRequirementCreateDTO(createDTO);
    InformationRequirement requirement = findInformationRequirementById(id);
    AccessForm accessForm = findAccessFormById(createDTO.getRequiredAccessFormId());
    requirement.setRequiredAccessForm(accessForm);
    requirement.setForEvent(createDTO.getForResourceEvent());
    return mapToDTO(requirementRepository.save(requirement));
  }

  @Override
  public InformationRequirementDTO getInformationRequirement(Long id) {
    return mapToDTO(findInformationRequirementById(id));
  }

  @Override
  public List<InformationRequirementDTO> getAllInformationRequirements() {
    return requirementRepository.findAll().stream().map(this::mapToDTO).toList();
  }

  @Override
  public void deleteInformationRequirement(Long id) {
    InformationRequirement requirement = findInformationRequirementById(id);
    requirementRepository.deleteById(requirement.getId());
  }

  private void validateInformationRequirementCreateDTO(InformationRequirementCreateDTO createDTO) {
    Objects.requireNonNull(createDTO, "InformationRequirementCreateDTO must not be null");
    Objects.requireNonNull(createDTO.getRequiredAccessFormId(), "Access form id must not be null");
  }

  private AccessForm findAccessFormById(Long id) {
    return accessFormRepository.findById(id).orElseThrow(() -> new EntityNotFoundException(id));
  }

  private InformationRequirement findInformationRequirementById(Long id) {
    return requirementRepository.findById(id).orElseThrow(() -> new EntityNotFoundException(id));
  }

  private InformationRequirementDTO mapToDTO(InformationRequirement informationRequirement) {
    return modelMapper.map(informationRequirement, InformationRequirementDTO.class);
  }
}
