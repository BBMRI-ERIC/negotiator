package eu.bbmri_eric.negotiator.service;

import eu.bbmri_eric.negotiator.database.model.ValueSet;
import eu.bbmri_eric.negotiator.database.repository.ValueSetRepository;
import eu.bbmri_eric.negotiator.dto.ValueSetCreateDTO;
import eu.bbmri_eric.negotiator.dto.ValueSetDto;
import eu.bbmri_eric.negotiator.exceptions.EntityNotFoundException;
import java.util.List;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class ValueSetServiceImpl implements ValueSetService {

  private final ValueSetRepository repository;
  private final ModelMapper mapper;

  public ValueSetServiceImpl(ValueSetRepository repository, ModelMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public List<ValueSetDto> getAllValueSets() {
    return repository.findAll(Sort.by("id").ascending()).stream()
        .map((element) -> mapper.map(element, ValueSetDto.class))
        .toList();
  }

  @Override
  public ValueSetDto getValueSetById(long id) {
    return mapper.map(
        repository.findById(id).orElseThrow(() -> new EntityNotFoundException(id)),
        ValueSetDto.class);
  }

  @Override
  public ValueSetDto createValueSet(ValueSetCreateDTO createDTO) {
    ValueSet valueSet = mapper.map(createDTO, ValueSet.class);
    return mapper.map(repository.save(valueSet), ValueSetDto.class);
  }

  @Override
  public ValueSetDto updateValueSet(ValueSetCreateDTO createDTO, Long id) {
    ValueSet valueSet = mapper.map(createDTO, ValueSet.class);
    valueSet.setId(id);
    return mapper.map(repository.save(valueSet), ValueSetDto.class);
  }
}
