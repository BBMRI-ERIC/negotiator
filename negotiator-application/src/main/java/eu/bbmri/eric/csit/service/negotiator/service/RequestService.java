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
import eu.bbmri.eric.csit.service.negotiator.repository.PersonRepository;
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
  @Autowired private PersonRepository personRepository;
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

  /**
   * Associates the Request entity with other Entities and create the record
   * @param requestEntity the Entity to save
   * @param queriesId a Set of query ids to associate to the Request
   * @param creatorId the ID of the Person that creates the Request (i.e., the authenticated Person that called the API)
   * @return The created query
   */
  private Request create(Request requestEntity, Set<Long> queriesId, Long creatorId) {
    // Gets the Entities for the queries
    Set<Query> queries = findQueries(queriesId);
    // Check if any query is already associated to a request
    if (queries.stream().anyMatch(query -> query.getRequest() != null)) {
      throw new WrongRequestException(
          "One or more query object is already assigned to another request");
    }

    // Updates the bidirectional relationship between query and request
    requestEntity.setQueries(new HashSet<>(queries));
    queries.forEach(
        query -> {
          query.setRequest(requestEntity);
        });

      // Gets the Role entity. Since this is a new request, the person is the CREATOR of the request
      Role role = roleRepository.findByName("CREATOR").orElseThrow(EntityNotStorableException::new);

      // Gets the person
      Person creator = personRepository.getById(creatorId);

      // Ceates the association between the Person and the Request
      PersonRequestRole personRole = new PersonRequestRole(creator, requestEntity, role);

      // Updates person and request with the person role
      creator.getRoles().add(personRole);
      requestEntity.getPersons().add(personRole);

    try {
      // Finally save the reques. NB: it also cascades operations for other Queries, PersonRequestRole
      requestRepository.save(requestEntity);
      return requestEntity;
    } catch (DataIntegrityViolationException ex) {
      throw new EntityNotStorableException();
    }
  }

  /**
   * Creates a Request into the repository. In this version the Request is created as part of an
   * already exisiting Project identified by the id
   * @param projectId the id of the project to which the Request has to be associated
   * @param request the RequestRequest DTO sent from to the endpoint
   * @param creatorId the ID of the Person that creates the Request (i.e., the authenticated Person that called the API)
   * @return the created Request entity
   */
  public Request create(Long projectId, RequestRequest request, Long creatorId) {
    Project project = projectService.findById(projectId);
    Request requestEntity = modelMapper.map(request, Request.class);
    requestEntity.setProject(project);
    project.getRequests().add(requestEntity);
    return create(requestEntity, request.getQueries(), creatorId);
  }

  /**
   * Creates a Request and the Project it is part of into the repository.
   * @param request the RequestRequest DTO sent from to the endpoint. It must have also the project data to create also the project
   * @param creatorId the ID of the Person that creates the Request (i.e., the authenticated Person that called the API)
   * @return the created Request entity
   */
  public Request create(RequestRequest request, Long creatorId) {
    if (request.getProject() == null) {
      throw new WrongRequestException("Missing project data");
    }
    Request requestEntity = modelMapper.map(request, Request.class);
    return create(requestEntity, request.getQueries(), creatorId);
  }

  /**
   * Updates the request with the specified ID.
   * @param id the id of the request tu update
   * @param request the RequestRequest DTO with the new Request data
   * @return The updated Request entity
   */
  public Request update(Long id, RequestRequest request) {
    Request requestEntity = findById(id);
    Set<Query> queries = findQueries(request.getQueries());

    if (queries.stream()
        .anyMatch(query -> query.getRequest() != null && query.getRequest() != requestEntity)) {
      throw new WrongRequestException(
          "One or more query object is already assigned to another request");
    }

    queries.forEach(
        query -> {
          query.setRequest(requestEntity);
        });

    requestEntity.setQueries(new HashSet<>(queries));
    requestEntity.setTitle(request.getTitle());
    requestEntity.setDescription(request.getDescription());

    try {
      requestRepository.save(requestEntity);
      return requestEntity;
    } catch (DataIntegrityViolationException ex) {
      throw new EntityNotStorableException();
    }
  }

  /**
   * Returns all request in the repository
   * @return the List of Request entities
   */
  public List<Request> findAll() {
    return requestRepository.findAll();
  }

  /**
   * Returns the Request with the specified id if exists, otherwise it throws an exception
   * @param id the id of the Request to retrieve
   * @return the Request with specified id
   */
  public Request findById(Long id) throws EntityNotFoundException {
    return requestRepository.findById(id).orElseThrow(() -> new EntityNotFoundException(id));
  }

  /**
   * Returns a List of Request entities filtered by biobank id
   * @param biobankId the id in the data source of the biobank of the request
   * @return the List of Request entities found
   */
  public List<Request> findByBiobankId(String biobankId) {
    return requestRepository.findByBiobankId(biobankId);
  }

  /**
   * Returns a List of Request entities filtered by biobank id
   * @param collectionId the id in the data source of the biobank of the request
   * @return the List of Request entities found
   */
  public List<Request> findByCollectionId(String collectionId) {
    return requestRepository.findByCollectionId(collectionId);
  }
}
