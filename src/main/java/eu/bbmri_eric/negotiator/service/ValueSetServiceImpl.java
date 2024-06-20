package eu.bbmri_eric.negotiator.service;

import eu.bbmri_eric.negotiator.database.model.ValueSet;
import eu.bbmri_eric.negotiator.database.repository.ValueSetRepository;
import eu.bbmri_eric.negotiator.dto.ValueSetCreateDTO;
import eu.bbmri_eric.negotiator.dto.ValueSetDTO;
import eu.bbmri_eric.negotiator.exceptions.EntityNotFoundException;
import jakarta.transaction.Transactional;
import java.util.List;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class ValueSetServiceImpl implements ValueSetService {

  private final ValueSetRepository repository;
  private final ModelMapper mapper;

  public ValueSetServiceImpl(ValueSetRepository repository, ModelMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public List<ValueSetDTO> getAllValueSets() {
    return repository.findAll(Sort.by("id").ascending()).stream()
        .map((element) -> mapper.map(element, ValueSetDTO.class))
        .toList();
  }

  @Override
  public ValueSetDTO getValueSetById(long id) {
    return mapper.map(
        repository.findById(id).orElseThrow(() -> new EntityNotFoundException(id)),
        ValueSetDTO.class);
  }

  @Override
  public ValueSetDTO createValueSet(ValueSetCreateDTO createDTO) {
    ValueSet valueSet = mapper.map(createDTO, ValueSet.class);
    return mapper.map(repository.save(valueSet), ValueSetDTO.class);
  }

  @Override
  public ValueSetDTO updateValueSet(ValueSetCreateDTO createDTO, Long id) {
    ValueSet valueSet = mapper.map(createDTO, ValueSet.class);
    valueSet.setId(id);
    return mapper.map(repository.save(valueSet), ValueSetDTO.class);
  }
}
