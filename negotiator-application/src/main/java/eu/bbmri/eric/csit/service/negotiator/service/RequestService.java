package eu.bbmri.eric.csit.service.negotiator.service;

import eu.bbmri.eric.csit.service.negotiator.dto.request.RequestRequest;
import eu.bbmri.eric.csit.service.negotiator.exceptions.EntityNotFoundException;
import eu.bbmri.eric.csit.service.negotiator.exceptions.EntityNotStorableException;
import eu.bbmri.eric.csit.service.negotiator.exceptions.WrongRequestException;
import eu.bbmri.eric.csit.service.negotiator.model.Person;
import eu.bbmri.eric.csit.service.negotiator.model.PersonRequestRole;
import eu.bbmri.eric.csit.service.negotiator.model.Project;
import eu.bbmri.eric.csit.service.negotiator.model.Query;
import eu.bbmri.eric.csit.service.negotiator.model.Request;
import eu.bbmri.eric.csit.service.negotiator.model.Role;
import eu.bbmri.eric.csit.service.negotiator.repository.PersonRequestRoleRepository;
import eu.bbmri.eric.csit.service.negotiator.repository.RequestRepository;
import eu.bbmri.eric.csit.service.negotiator.repository.RoleRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class RequestService {

  @Autowired private RequestRepository requestRepository;
  @Autowired private RoleRepository roleRepository;
  @Autowired private PersonRequestRoleRepository personRequestRoleRepository;
  @Autowired private ProjectService projectService;
  @Autowired private QueryService queryService;
  @Autowired private ModelMapper modelMapper;

  private Set<Query> findQueries(Set<Long> queriesId) {
    Set<Query> queries;
    try {
      queries = queryService.findAllById(queriesId);
    } catch (EntityNotFoundException ex) {
      throw new WrongRequestException("One or more of the specified queries do not exist");
    }
    return queries;
  }

  private Request create(Request requestEntity, Set<Long> queriesId, Person creator) {
    Set<Query> queries = findQueries(queriesId);

    if (queries.stream().anyMatch(query -> query.getRequest() != null)) {
      throw new WrongRequestException(
          "One or more query object is already assigned to another request");
    }

    try {
      requestRepository.save(requestEntity);

      Role role = roleRepository.findByName("CREATOR").orElseThrow(EntityNotStorableException::new);
      PersonRequestRole personRole = new PersonRequestRole();
      personRole.setPerson(creator);
      personRole.setRole(role);
      personRole.setRequest(requestEntity);
      personRequestRoleRepository.save(personRole);

      queries.forEach(
          query -> {
            query.setRequest(requestEntity);
            queryService.update(query);
          });

      requestEntity.setQueries(new HashSet<>(queries));
      requestEntity.setPersons(Set.of(personRole));
      return requestEntity;
    } catch (DataIntegrityViolationException ex) {
      throw new EntityNotStorableException();
    }
  }

  public Request create(Long projectId, RequestRequest request, Person creator) {
    Project project = projectService.findById(projectId);
    Request requestEntity = modelMapper.map(request, Request.class);
    requestEntity.setProject(project);
    return create(requestEntity, request.getQueries(), creator);
  }

  public Request create(RequestRequest request, Person creator) {
    if (request.getProject() == null) {
      throw new WrongRequestException("Missing project data");
    }
    Request requestEntity = modelMapper.map(request, Request.class);
    return create(requestEntity, request.getQueries(), creator);
  }

  public Request update(Long id, RequestRequest request) {
    Request requestEntity = findById(id);
    Set<Query> queries = findQueries(request.getQueries());

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

  public List<Request> findByBiobankId(String biobankId) {
    return requestRepository.findByBiobankId(biobankId);
  }

  public List<Request> findByCollectionId(String collectionId) {
    return requestRepository.findByCollectionId(collectionId);
  }
}
