package eu.bbmri_eric.negotiator.service;

import eu.bbmri_eric.negotiator.database.repository.AccessFormElementRepository;
import eu.bbmri_eric.negotiator.dto.access_form.AccessFormElementDTO;
import eu.bbmri_eric.negotiator.dto.access_form.ElementMetaDTO;
import eu.bbmri_eric.negotiator.exceptions.EntityNotFoundException;
import java.util.List;
import lombok.extern.apachecommons.CommonsLog;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

@Service
@CommonsLog
public class AccessFormElementServiceImpl implements AccessFormElementService {

  private final AccessFormElementRepository repository;
  private final ModelMapper mapper;

  public AccessFormElementServiceImpl(AccessFormElementRepository repository, ModelMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public List<ElementMetaDTO> getAll() {
    return repository.findAll().stream()
        .map((element) -> mapper.map(element, ElementMetaDTO.class))
        .toList();
  }

  @Override
  public AccessFormElementDTO getById(Long id) {
    return mapper.map(
        repository.findById(id).orElseThrow(() -> new EntityNotFoundException(id)),
        AccessFormElementDTO.class);
  }
}
