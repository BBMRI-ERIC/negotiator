package eu.bbmri.eric.csit.service.negotiator.service;

import eu.bbmri.eric.csit.service.negotiator.api.dto.negotiation.NegotiationCreateDTO;
import eu.bbmri.eric.csit.service.negotiator.api.dto.negotiation.NegotiationDTO;
import eu.bbmri.eric.csit.service.negotiator.database.model.*;
import eu.bbmri.eric.csit.service.negotiator.database.repository.NegotiationRepository;
import eu.bbmri.eric.csit.service.negotiator.database.repository.PersonRepository;
import eu.bbmri.eric.csit.service.negotiator.database.repository.RequestRepository;
import eu.bbmri.eric.csit.service.negotiator.database.repository.RoleRepository;
import eu.bbmri.eric.csit.service.negotiator.exceptions.EntityNotFoundException;
import eu.bbmri.eric.csit.service.negotiator.exceptions.EntityNotStorableException;
import eu.bbmri.eric.csit.service.negotiator.exceptions.WrongRequestException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.apachecommons.CommonsLog;
import org.hibernate.exception.DataException;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service(value = "DefaultNegotiationService")
@CommonsLog
public class NegotiationServiceImpl implements NegotiationService {

  @Autowired
  NegotiationRepository negotiationRepository;
  @Autowired
  RoleRepository roleRepository;
  @Autowired
  PersonRepository personRepository;
  @Autowired
  RequestRepository requestRepository;
  @Autowired
  ModelMapper modelMapper;
  @Autowired
  NegotiationStateService negotiationStateService;

  private List<Request> findRequests(Set<String> requestsId) {
    List<Request> entities;
    entities = requestRepository.findAllById(requestsId);
    if (entities.size() < requestsId.size()) {
      throw new WrongRequestException("One or more of the specified requests do not exist");
    }
    return entities;
  }

  @Override
  public boolean exists(String negotiationId) {
    try {
      findEntityById(negotiationId, false);
      return true;
    } catch (EntityNotFoundException ex) {
      return false;
    }
  }

  /**
   * Creates a Negotiation into the repository.
   *
   * @param negotiationBody the NegotiationCreateDTO DTO sent from to the endpoint
   * @param creatorId the ID of the Person that creates the Negotiation (i.e., the authenticated
   * Person that called the API)
   * @return the created Negotiation entity
   */
  @Transactional
  public NegotiationDTO create(NegotiationCreateDTO negotiationBody, Long creatorId) {
    Negotiation negotiationEntity = modelMapper.map(negotiationBody, Negotiation.class);
    // Gets the Entities for the requests
    log.debug("Getting request entities");

    List<Request> requests = findRequests(negotiationBody.getRequests());
    // Check if any negotiationBody is already associated to a negotiation
    if (requests.stream().anyMatch(request -> request.getNegotiation() != null)) {
      log.error("One or more negotiationBody object is already assigned to another negotiation");
      throw new WrongRequestException(
          "One or more negotiationBody object is already assigned to another negotiation");
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

    // Updates the bidirectional relationship between negotiationBody and negotiation
    negotiationEntity.setRequests(new HashSet<>(requests));
    requests.forEach(
        request -> {
          request.setNegotiation(negotiationEntity);
        });
    try {
      // Set initial state machine
      // Finally, save the negotiation. NB: it also cascades operations for other Requests,
      // PersonNegotiationRole
      Negotiation negotiation = negotiationRepository.save(negotiationEntity);
      negotiationStateService.createStateMachineForNegotiation(negotiationEntity.getId());
      return modelMapper.map(negotiation, NegotiationDTO.class);
    } catch (DataException | DataIntegrityViolationException ex) {
      log.error("Error while saving the Negotiation into db. Some db constraint violated");
      throw new EntityNotStorableException();
    }
  }

  private NegotiationDTO update(Negotiation negotiationEntity, NegotiationCreateDTO request) {
    List<Request> requests = findRequests(request.getRequests());

    if (requests.stream()
        .anyMatch(query -> query.getNegotiation() != null
            && query.getNegotiation() != negotiationEntity)) {
      throw new WrongRequestException(
          "One or more request object is already assigned to another negotiation");
    }

    requests.forEach(
        query -> {
          query.setNegotiation(negotiationEntity);
        });

    negotiationEntity.setRequests(new HashSet<>(requests));

    try {
      Negotiation negotiation = negotiationRepository.save(negotiationEntity);
      return modelMapper.map(negotiation, NegotiationDTO.class);
    } catch (DataException | DataIntegrityViolationException ex) {
      throw new EntityNotStorableException();
    }
  }

  /**
   * Updates the negotiation with the specified ID.
   *
   * @param negotiationId the negotiationId of the negotiation tu update
   * @param negotiationBody the NegotiationCreateDTO DTO with the new Negotiation data
   * @return The updated Negotiation entity
   */
  @Transactional
  public NegotiationDTO update(String negotiationId, NegotiationCreateDTO negotiationBody) {
    Negotiation negotiationEntity = findEntityById(negotiationId, true);
    return update(negotiationEntity, negotiationBody);
  }

  @Transactional
  public NegotiationDTO addRequestToNegotiation(String negotiationId, String requestId) {
    Negotiation negotiationEntity = findEntityById(negotiationId, false);
    Request requestEntity = requestRepository.getById(requestId);
    negotiationEntity.getRequests().add(requestEntity);
    requestEntity.setNegotiation(negotiationEntity);
    try {
      negotiationEntity = negotiationRepository.save(negotiationEntity);
      return modelMapper.map(negotiationEntity, NegotiationDTO.class);
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
  public List<NegotiationDTO> findAll() {
    List<Negotiation> negotiations = negotiationRepository.findAll();
    return negotiations.stream()
        .map(negotiation -> modelMapper.map(negotiation, NegotiationDTO.class))
        .collect(Collectors.toList());
  }

  private Negotiation findEntityById(String negotiationId, boolean includeDetails) {
    if (includeDetails) {
      return negotiationRepository
          .findDetailedById(negotiationId)
          .orElseThrow(() -> new EntityNotFoundException(negotiationId));
    } else {
      return negotiationRepository
          .findById(negotiationId)
          .orElseThrow(() -> new EntityNotFoundException(negotiationId));
    }
  }

  /**
   * Returns the Negotiation with the specified negotiationId if exists, otherwise it throws an
   * exception
   *
   * @param negotiationId the negotiationId of the Negotiation to retrieve
   * @param includeDetails whether the negotiation returned include details
   * @return the Negotiation with specified negotiationId
   */
  @Transactional
  public NegotiationDTO findById(String negotiationId, boolean includeDetails)
      throws EntityNotFoundException {
    Negotiation negotiation = findEntityById(negotiationId, includeDetails);
    return modelMapper.map(negotiation, NegotiationDTO.class);
  }

  /**
   * Returns a List of Negotiation entities filtered by biobank id
   *
   * @param biobankId the id in the data source of the biobank of the negotiation
   * @return the List of Negotiation entities found
   */
  @Transactional
  public List<NegotiationDTO> findByBiobankId(String biobankId) {
    List<Negotiation> negotiations = negotiationRepository.findByBiobankId(biobankId);
    return negotiations.stream()
        .map(negotiation -> modelMapper.map(negotiation, NegotiationDTO.class))
        .collect(Collectors.toList());
  }

  /**
   * Returns a List of Negotiation entities filtered by biobank id
   *
   * @param collectionId the id in the data source of the biobank of the negotiation
   * @return the List of Negotiation entities found
   */
  @Transactional
  public List<NegotiationDTO> findByCollectionId(String collectionId) {
    List<Negotiation> negotiations = negotiationRepository.findByCollectionId(collectionId);
    return negotiations.stream()
        .map(negotiation -> modelMapper.map(negotiation, NegotiationDTO.class))
        .collect(Collectors.toList());
  }

  @Transactional
  public List<NegotiationDTO> findByUserIdAndRole(String userId, String userRole) {
    List<Negotiation> negotiations = negotiationRepository.findByUserIdAndRole(userId, userRole);
    return negotiations.stream()
        .map(negotiation -> modelMapper.map(negotiation, NegotiationDTO.class))
        .collect(Collectors.toList());
  }

  @Override
  public NegotiationDTO changeState(String negotiationId, NegotiationEvent negotiationEvent) {
    NegotiationState newState = negotiationStateService.sendEvent(negotiationId, negotiationEvent);
    return null;
  }
}
