package eu.bbmri_eric.negotiator.service;

import eu.bbmri_eric.negotiator.database.model.AccessCriteriaSet;
import eu.bbmri_eric.negotiator.database.model.Request;
import eu.bbmri_eric.negotiator.database.model.Resource;
import eu.bbmri_eric.negotiator.database.repository.RequestRepository;
import eu.bbmri_eric.negotiator.dto.access_criteria.AccessCriteriaSetDTO;
import eu.bbmri_eric.negotiator.exceptions.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

@Service
public class AccessFormServiceImpl implements AccessFormService {
  RequestRepository requestRepository;
  ModelMapper modelMapper;

  public AccessFormServiceImpl(RequestRepository requestRepository, ModelMapper modelMapper) {
    this.requestRepository = requestRepository;
    this.modelMapper = modelMapper;
  }

  @Override
  @Transactional
  public AccessCriteriaSetDTO getAccessFormForRequest(String requestId)
      throws EntityNotFoundException {
    if (requestId == null) {
      throw new IllegalArgumentException("Request id cannot be null");
    }
    Request request =
        requestRepository
            .findById(requestId)
            .orElseThrow(() -> new EntityNotFoundException(requestId));
    AccessCriteriaSet accessCriteriaSet =
        request.getResources().iterator().next().getAccessCriteriaSet();
    for (Resource resource : request.getResources()) {
      accessCriteriaSet.getSections().addAll(resource.getAccessCriteriaSet().getSections());
    }
    return modelMapper.map(
        request.getResources().iterator().next().getAccessCriteriaSet(),
        AccessCriteriaSetDTO.class);
  }
}
