package eu.bbmri.eric.csit.service.negotiator.service;

import eu.bbmri.eric.csit.service.model.Project;
import eu.bbmri.eric.csit.service.model.Request;
import eu.bbmri.eric.csit.service.negotiator.dto.request.RequestRequest;
import eu.bbmri.eric.csit.service.negotiator.exceptions.EntityNotFoundException;
import eu.bbmri.eric.csit.service.negotiator.exceptions.EntityNotStorableException;
import eu.bbmri.eric.csit.service.negotiator.exceptions.WrongRequestException;
import eu.bbmri.eric.csit.service.repository.RequestRepository;
import java.util.List;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class RequestService {

  @Autowired private RequestRepository requestRepository;
  @Autowired private ProjectService projectService;
  @Autowired private ModelMapper modelMapper;

  public Request create(Project project, RequestRequest request) {
    Request requestEntity = modelMapper.map(request, Request.class);
    requestEntity.setProject(project);
    requestEntity.setToken("token");
    try {
      return requestRepository.save(requestEntity);
    } catch (DataIntegrityViolationException ex) {
      throw new EntityNotStorableException();
    }
  }

  public Request create(RequestRequest request) {
    if (request.getProject() == null) {
      throw new WrongRequestException();
    }
    Project project = projectService.create(request.getProject());
    return create(project, request);
  }

  public List<Request> findAll() {
    return requestRepository.findAll();
  }

  public Request findById(Long id) {
    return requestRepository.findById(id).orElseThrow(() -> new EntityNotFoundException(id));
  }
}
