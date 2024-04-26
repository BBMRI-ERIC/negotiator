package eu.bbmri_eric.negotiator.service;

import eu.bbmri_eric.negotiator.database.model.AccessFormElement;
import eu.bbmri_eric.negotiator.database.model.ValueSet;
import eu.bbmri_eric.negotiator.database.repository.AccessFormElementRepository;
import eu.bbmri_eric.negotiator.database.repository.ValueSetRepository;
import eu.bbmri_eric.negotiator.dto.access_form.ElementCreateDTO;
import eu.bbmri_eric.negotiator.dto.access_form.ElementMetaDTO;
import eu.bbmri_eric.negotiator.exceptions.EntityNotFoundException;
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

  @Override
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
  public ElementMetaDTO createElement(ElementCreateDTO elementCreateDTO) {
    AccessFormElement element = mapper.map(elementCreateDTO, AccessFormElement.class);
    if (Objects.nonNull(elementCreateDTO.getValueSetId())) {
      ValueSet valueSet =
          valueSetRepository
              .findById(elementCreateDTO.getValueSetId())
              .orElseThrow(() -> new EntityNotFoundException(elementCreateDTO.getValueSetId()));
      element.setLinkedValueSet(valueSet);
    }
    return mapper.map(repository.save(element), ElementMetaDTO.class);
  }

  @Override
  public ElementMetaDTO updateElement(ElementCreateDTO elementCreateDTO, Long id) {
    AccessFormElement element = mapper.map(elementCreateDTO, AccessFormElement.class);
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
