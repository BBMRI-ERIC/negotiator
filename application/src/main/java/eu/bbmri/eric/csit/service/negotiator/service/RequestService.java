package eu.bbmri.eric.csit.service.negotiator.service;

import eu.bbmri.eric.csit.service.model.Project;
import eu.bbmri.eric.csit.service.model.Query;
import eu.bbmri.eric.csit.service.model.Request;
import eu.bbmri.eric.csit.service.negotiator.dto.request.RequestRequest;
import eu.bbmri.eric.csit.service.negotiator.exceptions.EntityNotFoundException;
import eu.bbmri.eric.csit.service.negotiator.exceptions.EntityNotStorableException;
import eu.bbmri.eric.csit.service.negotiator.exceptions.WrongRequestException;
import eu.bbmri.eric.csit.service.repository.RequestRepository;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class RequestService {

  @Autowired private RequestRepository requestRepository;
  @Autowired private ProjectService projectService;
  @Autowired private QueryService queryService;
  @Autowired private ModelMapper modelMapper;

  private Request create(Project project, RequestRequest request) {
    List<Query> queries;
    try {
      queries = queryService.findAllById(request.getQueries());
    } catch (EntityNotFoundException ex) {
      throw new WrongRequestException("One or more of the specified queries do not exist");
    }

    Request requestEntity = modelMapper.map(request, Request.class);
    requestEntity.setProject(project);
    requestEntity.setQueries(new HashSet<>(queries));

    try {
      return requestRepository.save(requestEntity);
    } catch (DataIntegrityViolationException ex) {
      throw new EntityNotStorableException();
    }
  }

  public Request create(Long projectId, RequestRequest request) {
    Project project = projectService.findById(projectId);
    return create(project, request);
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
