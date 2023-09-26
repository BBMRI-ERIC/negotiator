package eu.bbmri.eric.csit.service.negotiator.service;

import eu.bbmri.eric.csit.service.negotiator.database.model.AccessCriteriaSet;
import eu.bbmri.eric.csit.service.negotiator.database.repository.AccessCriteriaSetRepository;
import eu.bbmri.eric.csit.service.negotiator.dto.access_criteria.AccessCriteriaSetDTO;
import eu.bbmri.eric.csit.service.negotiator.exceptions.EntityNotFoundException;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service(value = "DefaultAccessCriteriaSetService")
public class AccessCriteriaSetServiceImpl implements AccessCriteriaSetService {

  private final AccessCriteriaSetRepository accessCriteriaSetRepository;

  private final ModelMapper modelMapper;

  public AccessCriteriaSetServiceImpl(
      AccessCriteriaSetRepository accessCriteriaSetRepository, ModelMapper modelMapper) {
    this.accessCriteriaSetRepository = accessCriteriaSetRepository;
    this.modelMapper = modelMapper;
  }

  @Transactional
  public AccessCriteriaSetDTO findByResourceId(String resourceEntityId) {
    AccessCriteriaSet acs =
        accessCriteriaSetRepository
            .findByResourceId(resourceEntityId)
            .orElseThrow(() -> new EntityNotFoundException(resourceEntityId));

    return modelMapper.map(acs, AccessCriteriaSetDTO.class);
  }
}
