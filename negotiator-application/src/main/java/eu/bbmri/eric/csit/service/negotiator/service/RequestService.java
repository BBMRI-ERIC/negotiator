package eu.bbmri.eric.csit.service.negotiator.service;

import eu.bbmri.eric.csit.service.negotiator.dto.request.RequestRequest;
import eu.bbmri.eric.csit.service.negotiator.exceptions.EntityNotFoundException;
import eu.bbmri.eric.csit.service.negotiator.exceptions.EntityNotStorableException;
import eu.bbmri.eric.csit.service.negotiator.exceptions.WrongRequestException;
import eu.bbmri.eric.csit.service.negotiator.model.Project;
import eu.bbmri.eric.csit.service.negotiator.model.Query;
import eu.bbmri.eric.csit.service.negotiator.model.Request;
import eu.bbmri.eric.csit.service.negotiator.repository.RequestRepository;
import java.util.HashSet;
import java.util.List;
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

  private List<Query> findQueries(List<Long> queriesId) {
    List<Query> queries;
    try {
      queries = queryService.findAllById(queriesId);
    } catch (EntityNotFoundException ex) {
      throw new WrongRequestException("One or more of the specified queries do not exist");
    }
    return queries;
  }

  private Request create(Project project, RequestRequest request) {
    List<Query> queries = findQueries(request.getQueries());

    final Request requestEntity = modelMapper.map(request, Request.class);
    requestEntity.setProject(project);

    if (queries.stream().anyMatch(query -> query.getRequest() != null)) {
      throw new WrongRequestException(
          "One or more query object is already assigned to another request");
    }
    try {

      requestRepository.save(requestEntity);
      queries.forEach(
          query -> {
            query.setRequest(requestEntity);
            queryService.update(query);
          });
      requestEntity.setQueries(new HashSet<>(queries));
      return requestEntity;
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
      throw new WrongRequestException("Missing project data");
    }
    Project project = projectService.create(request.getProject());
    return create(project, request);
  }

  public Request update(Long id, RequestRequest request) {
    final Request requestEntity = findById(id);

    List<Query> queries = findQueries(request.getQueries());

    if (queries.stream()
        .anyMatch(query -> query.getRequest() != null && query.getRequest() != requestEntity)) {
      throw new WrongRequestException(
          "One or more query object is already assigned to another request");
    }

    requestEntity.setTitle(request.getTitle());
    requestEntity.setDescription(request.getDescription());

    try {
      requestRepository.save(requestEntity);
      queries.forEach(
          query -> {
            query.setRequest(requestEntity);
            queryService.update(query);
          });
      requestEntity.setQueries(new HashSet<>(queries));
      return requestEntity;
    } catch (DataIntegrityViolationException ex) {
      throw new EntityNotStorableException();
    }
  }

  public List<Request> findAll() {
    return requestRepository.findAll();
  }

  public Request findById(Long id) {
    return requestRepository.findById(id).orElseThrow(() -> new EntityNotFoundException(id));
  }
}
