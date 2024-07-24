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
  private InformationRequirementRepository requirementRepository;
  private AccessFormRepository accessFormRepository;
  private ModelMapper modelMapper;

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
    Objects.requireNonNull(createDTO, "InformationRequirementCreateDTO must not be null");
    Objects.requireNonNull(createDTO.getRequiredAccessFormId(), "Access form id must not be null");
    AccessForm accessForm =
        accessFormRepository
            .findById(createDTO.getRequiredAccessFormId())
            .orElseThrow(() -> new EntityNotFoundException(createDTO.getRequiredAccessFormId()));
    InformationRequirement requirement =
        new InformationRequirement(accessForm, createDTO.getForResourceState());
    return modelMapper.map(
        requirementRepository.save(requirement), InformationRequirementDTO.class);
  }

  @Override
  public InformationRequirementDTO updateInformationRequirement(
      InformationRequirementCreateDTO createDTO, Long id) {
    Objects.requireNonNull(createDTO, "InformationRequirementCreateDTO must not be null");
    InformationRequirement requirement =
        requirementRepository.findById(id).orElseThrow(() -> new EntityNotFoundException(id));
    AccessForm accessForm =
        accessFormRepository
            .findById(createDTO.getRequiredAccessFormId())
            .orElseThrow(() -> new EntityNotFoundException(createDTO.getRequiredAccessFormId()));
    requirement.setRequiredAccessForm(accessForm);
    requirement.setForResourceState(createDTO.getForResourceState());
    return modelMapper.map(
        requirementRepository.save(requirement), InformationRequirementDTO.class);
  }

  @Override
  public InformationRequirementDTO getInformationRequirement(Long id) {
    Objects.requireNonNull(id, "InformationRequirement ID must not be null");
    return modelMapper.map(
        requirementRepository.findById(id).orElseThrow(() -> new EntityNotFoundException(id)),
        InformationRequirementDTO.class);
  }

  @Override
  public List<InformationRequirementDTO> getAllInformationRequirements() {
    return requirementRepository.findAll().stream()
        .map(
            informationRequirement ->
                modelMapper.map(informationRequirement, InformationRequirementDTO.class))
        .toList();
  }

  @Override
  public void deleteInformationRequirement(Long id) {
    Objects.requireNonNull(id, "InformationRequirement ID must not be null");
    requirementRepository.deleteById(id);
  }
}
