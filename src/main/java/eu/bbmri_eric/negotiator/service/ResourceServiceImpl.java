package eu.bbmri_eric.negotiator.service;

import eu.bbmri_eric.negotiator.database.repository.ResourceRepository;
import eu.bbmri_eric.negotiator.dto.person.ResourceResponseModel;
import eu.bbmri_eric.negotiator.exceptions.EntityNotFoundException;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class ResourceServiceImpl implements ResourceService {
  private final ResourceRepository repository;
  private final ModelMapper modelMapper;

  public ResourceServiceImpl(ResourceRepository repository, ModelMapper modelMapper) {
    this.repository = repository;
    this.modelMapper = modelMapper;
  }

  @Override
  public ResourceResponseModel findById(Long id) {
    return modelMapper.map(
        repository.findById(id).orElseThrow(() -> new EntityNotFoundException(id)),
        ResourceResponseModel.class);
  }

  @Override
  public Iterable<ResourceResponseModel> findAll(Pageable pageable) {
    return repository
        .findAll(pageable)
        .map(resource -> modelMapper.map(resource, ResourceResponseModel.class));
  }
}
