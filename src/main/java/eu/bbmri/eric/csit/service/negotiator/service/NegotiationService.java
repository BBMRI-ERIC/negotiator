package eu.bbmri.eric.csit.service.negotiator.service;

import eu.bbmri.eric.csit.service.negotiator.api.dto.negotiation.NegotiationCreateDTO;
import eu.bbmri.eric.csit.service.negotiator.database.model.*;
import eu.bbmri.eric.csit.service.negotiator.database.repository.TempRequestRepository;
import eu.bbmri.eric.csit.service.negotiator.exceptions.EntityNotFoundException;
import eu.bbmri.eric.csit.service.negotiator.exceptions.EntityNotStorableException;
import eu.bbmri.eric.csit.service.negotiator.exceptions.WrongRequestException;
import eu.bbmri.eric.csit.service.negotiator.database.repository.PersonRepository;
import eu.bbmri.eric.csit.service.negotiator.database.repository.NegotiationRepository;
import eu.bbmri.eric.csit.service.negotiator.database.repository.RoleRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.extern.apachecommons.CommonsLog;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.hibernate.exception.DataException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@CommonsLog
public class NegotiationService {

  @Autowired private NegotiationRepository negotiationRepository;

  private TempRequestRepository tempRequestRepository;
  @Autowired private RoleRepository roleRepository;
  @Autowired private PersonRepository personRepository;
  @Autowired private ProjectService projectService;
  @Autowired private RequestService requestService;
  @Autowired private ModelMapper modelMapper;

  private Set<Request> findQueries(Set<String> queriesId) {
    Set<Request> queries;
    try {
      queries = requestService.findAllById(queriesId);
    } catch (EntityNotFoundException ex) {
      log.error("Some of the specified queries where not found");
      throw new WrongRequestException("One or more of the specified queries do not exist");
    }
    return queries;
  }

  public void createRequest(NegotiationRequest negotiationRequest){
    log.debug(negotiationRequest.toString());
    tempRequestRepository.save(negotiationRequest);
  }
  public NegotiationRequest getNegotiationRequestById(Long id){
    return tempRequestRepository.findById(id);
  }

  /**
   * Associates the Negotiation entity with other Entities and create the record
   *
   * @param negotiationEntity the Entity to save
   * @param queriesId a Set of request ids to associate to the Negotiation
   * @param creatorId the ID of the Person that creates the Negotiation (i.e., the authenticated Person
   *     that called the API)
   * @return The created request
   */
  private Negotiation create(Negotiation negotiationEntity, Set<String> queriesId, Long creatorId) {
    // Gets the Entities for the queries
    log.debug("Getting request entities");
    Set<Request> queries = findQueries(queriesId);
    // Check if any request is already associated to a negotiation
    if (queries.stream().anyMatch(query -> query.getNegotiation() != null)) {
      log.error("One or more request object is already assigned to another negotiation");
      throw new WrongRequestException(
          "One or more request object is already assigned to another negotiation");
    }

    // Gets the Role entity. Since this is a new negotiation, the person is the CREATOR of the negotiation
    Role role = roleRepository.findByName("CREATOR").orElseThrow(EntityNotStorableException::new);

    // Gets the person and associated roles
    Person creator =
        personRepository.findDetailedById(creatorId).orElseThrow(EntityNotStorableException::new);

    // Ceates the association between the Person and the Negotiation
    PersonNegotiationRole personRole = new PersonNegotiationRole(creator, negotiationEntity, role);

    // Updates person and negotiation with the person role
    creator.getRoles().add(personRole);
    negotiationEntity.getPersons().add(personRole);

    // Updates the bidirectional relationship between request and negotiation
    negotiationEntity.setQueries(new HashSet<>(queries));
    queries.forEach(
        query -> {
          query.setNegotiation(negotiationEntity);
        });

    try {
      // Finally, save the negotiation. NB: it also cascades operations for other Queries,
      // PersonNegotiationRole
      return negotiationRepository.save(negotiationEntity);
    } catch (DataException | DataIntegrityViolationException ex) {
      log.error("Error while saving the Negotiation into db. Some db constraint violated");
      throw new EntityNotStorableException();
    }
  }

  /**
   * Creates a Negotiation into the repository. In this version the Negotiation is created as part of an
   * already exisiting Project identified by the id
   *
   * @param projectId the id of the project to which the Negotiation has to be associated
   * @param request the NegotiationCreateDTO DTO sent from to the endpoint
   * @param creatorId the ID of the Person that creates the Negotiation (i.e., the authenticated Person
   *     that called the API)
   * @return the created Negotiation entity
   */
  @Transactional
  public Negotiation create(String projectId, NegotiationCreateDTO request, Long creatorId) {
    // Get the project or throw an exception
    Project project = projectService.findById(projectId);
    Negotiation negotiationEntity = modelMapper.map(request, Negotiation.class);
    negotiationEntity.setProject(project);
    project.getNegotiations().add(negotiationEntity);
    return create(negotiationEntity, request.getQueries(), creatorId);
  }

  /**
   * Creates a Negotiation and the Project it is part of into the repository.
   *
   * @param request the NegotiationCreateDTO DTO sent from to the endpoint. It must have also the project
   *     data to create also the project
   * @param creatorId the ID of the Person that creates the Negotiation (i.e., the authenticated Person
   *     that called the API)
   * @return the created Negotiation entity
   */
  @Transactional
  public Negotiation create(NegotiationCreateDTO request, Long creatorId) {
    if (request.getProject() == null) {
      throw new WrongRequestException("Missing project data");
    }
    Negotiation negotiationEntity = modelMapper.map(request, Negotiation.class);
    return create(negotiationEntity, request.getQueries(), creatorId);
  }

  private Negotiation update(Negotiation negotiationEntity, NegotiationCreateDTO request) {
    Set<Request> queries = findQueries(request.getQueries());

    if (queries.stream()
        .anyMatch(query -> query.getNegotiation() != null && query.getNegotiation() != negotiationEntity)) {
      throw new WrongRequestException(
          "One or more request object is already assigned to another negotiation");
    }

    queries.forEach(
        query -> {
          query.setNegotiation(negotiationEntity);
        });

    negotiationEntity.setQueries(new HashSet<>(queries));
    negotiationEntity.setTitle(request.getTitle());
    negotiationEntity.setDescription(request.getDescription());

    try {
      negotiationRepository.save(negotiationEntity);
      return negotiationEntity;
    } catch (DataException | DataIntegrityViolationException ex) {
      throw new EntityNotStorableException();
    }
  }

  /**
   * Updates the negotiation with the specified ID.
   *
   * @param id the id of the negotiation tu update
   * @param request the NegotiationCreateDTO DTO with the new Negotiation data
   * @return The updated Negotiation entity
   */
  @Transactional
  public Negotiation update(String id, NegotiationCreateDTO request) {
    Negotiation negotiationEntity = findDetailedById(id);
    return update(negotiationEntity, request);
  }

  @Transactional
  public Negotiation addQueryToRequest(String id, Request requestEntity) {
    Negotiation negotiationEntity = findById(id);
    negotiationEntity.getQueries().add(requestEntity);
    requestEntity.setNegotiation(negotiationEntity);
    try {
      negotiationRepository.save(negotiationEntity);
      return negotiationEntity;
    } catch (DataIntegrityViolationException ex) {
      throw new EntityNotStorableException();
    }
  }

  /**
   * Returns all negotiation in the repository
   *
   * @return the List of Negotiation entities
   */
  @Transactional
  public List<Negotiation> findAll() {
    return negotiationRepository.findAll();
  }

  /**
   * Returns the Negotiation with the specified id if exists, otherwise it throws an exception
   *
   * @param id the id of the Negotiation to retrieve
   * @return the Negotiation with specified id
   */
  @Transactional
  public Negotiation findDetailedById(String id) throws EntityNotFoundException {
    return negotiationRepository
        .findDetailedById(id)
        .orElseThrow(() -> new EntityNotFoundException(id));
  }

  /**
   * Returns the Negotiation with the specified id if exists, otherwise it throws an exception
   *
   * @param id the id of the Negotiation to retrieve
   * @return the Negotiation with specified id
   */
  @Transactional
  public Negotiation findById(String id) throws EntityNotFoundException {
    return negotiationRepository
        .findById(id)
        .orElseThrow(() -> new EntityNotFoundException(id));
  }

  /**
   * Returns a List of Negotiation entities filtered by biobank id
   *
   * @param biobankId the id in the data source of the biobank of the negotiation
   * @return the List of Negotiation entities found
   */
  @Transactional
  public List<Negotiation> findByBiobankId(String biobankId) {
    return negotiationRepository.findByBiobankId(biobankId);
  }

  /**
   * Returns a List of Negotiation entities filtered by biobank id
   *
   * @param collectionId the id in the data source of the biobank of the negotiation
   * @return the List of Negotiation entities found
   */
  @Transactional
  public List<Negotiation> findByCollectionId(String collectionId) {
    return negotiationRepository.findByCollectionId(collectionId);
  }
}
