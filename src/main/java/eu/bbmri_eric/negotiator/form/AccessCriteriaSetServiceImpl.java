package eu.bbmri_eric.negotiator.form;

import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.form.dto.AccessFormDTO;
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
  public AccessFormDTO findByResourceId(String resourceEntityId) {
    AccessForm acs =
        accessFormRepository
            .findByResourceId(resourceEntityId)
            .orElseThrow(() -> new EntityNotFoundException(resourceEntityId));
    return modelMapper.map(acs, AccessFormDTO.class);
  }
}
