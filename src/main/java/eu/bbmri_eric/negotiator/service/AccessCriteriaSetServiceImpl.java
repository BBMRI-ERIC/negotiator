package eu.bbmri_eric.negotiator.service;

import eu.bbmri_eric.negotiator.database.model.AccessForm;
import eu.bbmri_eric.negotiator.database.repository.AccessFormRepository;
import eu.bbmri_eric.negotiator.dto.access_criteria.AccessCriteriaSetDTO;
import eu.bbmri_eric.negotiator.exceptions.EntityNotFoundException;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service(value = "DefaultAccessCriteriaSetService")
public class AccessCriteriaSetServiceImpl implements AccessCriteriaSetService {

  private final AccessFormRepository accessFormRepository;

  private final ModelMapper modelMapper;

  public AccessCriteriaSetServiceImpl(
      AccessFormRepository accessFormRepository, ModelMapper modelMapper) {
    this.accessFormRepository = accessFormRepository;
    this.modelMapper = modelMapper;
  }

  @Transactional
  public AccessCriteriaSetDTO findByResourceId(String resourceEntityId) {
    AccessForm acs =
        accessFormRepository
            .findByResourceId(resourceEntityId)
            .orElseThrow(() -> new EntityNotFoundException(resourceEntityId));

    return modelMapper.map(acs, AccessCriteriaSetDTO.class);
  }
}
