package eu.bbmri_eric.negotiator.form.service;

import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.form.AccessFormElement;
import eu.bbmri_eric.negotiator.form.FormElementType;
import eu.bbmri_eric.negotiator.form.dto.ElementCreateDTO;
import eu.bbmri_eric.negotiator.form.dto.ElementMetaDTO;
import eu.bbmri_eric.negotiator.form.repository.AccessFormElementRepository;
import eu.bbmri_eric.negotiator.form.value_set.ValueSet;
import eu.bbmri_eric.negotiator.form.value_set.ValueSetRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Objects;
import lombok.extern.apachecommons.CommonsLog;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@CommonsLog
public class AccessFormElementServiceImpl implements AccessFormElementService {

  private final AccessFormElementRepository repository;
  private final ValueSetRepository valueSetRepository;
  private final ModelMapper mapper;

  public AccessFormElementServiceImpl(
      AccessFormElementRepository repository,
      ValueSetRepository valueSetRepository,
      ModelMapper mapper) {
    this.repository = repository;
    this.valueSetRepository = valueSetRepository;
    this.mapper = mapper;
  }

  private static void verifyTypeAndValueSetCombination(ElementCreateDTO elementCreateDTO) {
    if ((elementCreateDTO.getType() == FormElementType.MULTIPLE_CHOICE
            || elementCreateDTO.getType() == FormElementType.SINGLE_CHOICE)
        && (elementCreateDTO.getValueSetId() == null
            || elementCreateDTO.getValueSetId().equals(0L))) {
      throw new IllegalArgumentException("The chosen element type must have a value set");
    }
  }

  @Override
  @Transactional
  public List<ElementMetaDTO> getAllElements() {
    return repository.findAll(Sort.by("id").ascending()).stream()
        .map((element) -> mapper.map(element, ElementMetaDTO.class))
        .toList();
  }

  @Override
  public ElementMetaDTO getElementById(Long id) {
    return mapper.map(
        repository.findById(id).orElseThrow(() -> new EntityNotFoundException(id)),
        ElementMetaDTO.class);
  }

  @Override
  @Transactional
  public ElementMetaDTO createElement(ElementCreateDTO elementCreateDTO) {
    verifyTypeAndValueSetCombination(elementCreateDTO);
    AccessFormElement element =
        new AccessFormElement(
            elementCreateDTO.getName(),
            elementCreateDTO.getLabel(),
            elementCreateDTO.getDescription(),
            elementCreateDTO.getType());
    AccessFormElement savedElement = repository.save(element);
    if (Objects.nonNull(elementCreateDTO.getValueSetId())) {
      ValueSet valueSet =
          valueSetRepository
              .findById(elementCreateDTO.getValueSetId())
              .orElseThrow(() -> new EntityNotFoundException(elementCreateDTO.getValueSetId()));
      savedElement.setLinkedValueSet(valueSet);
    }
    return mapper.map(savedElement, ElementMetaDTO.class);
  }

  @Override
  @Transactional
  public ElementMetaDTO updateElement(ElementCreateDTO elementCreateDTO, Long id) {
    verifyTypeAndValueSetCombination(elementCreateDTO);
    AccessFormElement element =
        repository.findById(id).orElseThrow(() -> new EntityNotFoundException(id));
    element = mapper.map(elementCreateDTO, AccessFormElement.class);
    element.setId(id);
    if (Objects.nonNull(elementCreateDTO.getValueSetId())) {
      ValueSet valueSet =
          valueSetRepository
              .findById(elementCreateDTO.getValueSetId())
              .orElseThrow(() -> new EntityNotFoundException(elementCreateDTO.getValueSetId()));
      element.setLinkedValueSet(valueSet);
    }
    return mapper.map(repository.save(element), ElementMetaDTO.class);
  }
}
