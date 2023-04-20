package eu.bbmri.eric.csit.service.negotiator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri.eric.csit.service.negotiator.api.dto.negotiation.NegotiationCreateDTO;
import eu.bbmri.eric.csit.service.negotiator.api.dto.negotiation.NegotiationDTO;
import eu.bbmri.eric.csit.service.negotiator.api.dto.person.PersonRoleDTO;
import eu.bbmri.eric.csit.service.negotiator.database.model.Negotiation;
import eu.bbmri.eric.csit.service.negotiator.database.model.Person;
import eu.bbmri.eric.csit.service.negotiator.database.model.PersonNegotiationRole;
import eu.bbmri.eric.csit.service.negotiator.database.model.Request;
import eu.bbmri.eric.csit.service.negotiator.database.model.Role;
import eu.bbmri.eric.csit.service.negotiator.database.repository.NegotiationRepository;
import eu.bbmri.eric.csit.service.negotiator.database.repository.PersonRepository;
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
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@CommonsLog
public class NegotiationServiceImpl  implements NegotiationService {

  private final NegotiationRepository negotiationRepository;
  private final RoleRepository roleRepository;
  private final PersonRepository personRepository;
  private final RequestService requestService;
  private final ModelMapper modelMapper;

  public NegotiationServiceImpl(NegotiationRepository negotiationRepository,
      RoleRepository roleRepository, PersonRepository personRepository,
      RequestService requestService,
      ModelMapper modelMapper) {

    this.negotiationRepository = negotiationRepository;
    this.roleRepository = roleRepository;
    this.personRepository = personRepository;
    this.requestService = requestService;
    this.modelMapper = modelMapper;

    TypeMap<Negotiation, NegotiationDTO> typeMap =
        modelMapper.createTypeMap(Negotiation.class, NegotiationDTO.class);

    Converter<Set<PersonNegotiationRole>, Set<PersonRoleDTO>> personsRoleConverter =
        role -> personsRoleConverter(role.getSource());

    typeMap.addMappings(
        mapper ->
            mapper
                .using(personsRoleConverter)
                .map(Negotiation::getPersons, NegotiationDTO::setPersons));

    Converter<String, JsonNode> payloadConverter =
        p -> {
          try {
            return payloadConverter(p.getSource());
          } catch (JsonProcessingException e) {
            throw new RuntimeException(e);  // TODO: raise the correct exception
          }
        };

    typeMap.addMappings(mapper -> mapper.using(payloadConverter)
        .map(Negotiation::getPayload, NegotiationDTO::setPayload));

  }

  private Set<PersonRoleDTO> personsRoleConverter(Set<PersonNegotiationRole> personsRoles) {
    Set<PersonRoleDTO> pr = personsRoles.stream()
        .map(
            personRole ->
                new PersonRoleDTO(
                    personRole.getPerson().getAuthName(), personRole.getRole().getName()))
        .collect(Collectors.toSet());
    return pr;
  }

  private JsonNode payloadConverter(String jsonPayload) throws JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();
    return mapper.readTree(jsonPayload);
  }

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

  /**
   * Associates the Negotiation entity with other Entities and create the record
   *
   * @param negotiationEntity the Entity to save
   * @param queriesId a Set of request ids to associate to the Negotiation
   * @param creatorId the ID of the Person that creates the Negotiation (i.e., the authenticated
   * Person that called the API)
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
    negotiationEntity.setRequests(new HashSet<>(queries));
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
   * @param request the NegotiationCreateDTO DTO sent from to the endpoint
   * @param creatorId the ID of the Person that creates the Negotiation (i.e., the authenticated
   * Person that called the API)
   * @return the created Negotiation entity
   */
  @Transactional
  public NegotiationDTO create(NegotiationCreateDTO request, Long creatorId) {
    Negotiation negotiationEntity = modelMapper.map(request, Negotiation.class);
    // Gets the Entities for the queries
    log.debug("Getting request entities");

    Set<Request> queries = findQueries(request.getRequests());
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
    negotiationEntity.setRequests(new HashSet<>(queries));
    queries.forEach(
        query -> {
          query.setNegotiation(negotiationEntity);
        });

    try {
      // Finally, save the negotiation. NB: it also cascades operations for other Requests,
      // PersonNegotiationRole
      Negotiation negotiation = negotiationRepository.save(negotiationEntity);
      return modelMapper.map(negotiation, NegotiationDTO.class);
    } catch (DataException | DataIntegrityViolationException ex) {
      log.error("Error while saving the Negotiation into db. Some db constraint violated");
      throw new EntityNotStorableException();
    }
  }

  private NegotiationDTO update(Negotiation negotiationEntity, NegotiationCreateDTO request) {
    Set<Request> queries = findQueries(request.getRequests());

    if (queries.stream()
        .anyMatch(query -> query.getNegotiation() != null
            && query.getNegotiation() != negotiationEntity)) {
      throw new WrongRequestException(
          "One or more request object is already assigned to another negotiation");
    }

    queries.forEach(
        query -> {
          query.setNegotiation(negotiationEntity);
        });

    negotiationEntity.setRequests(new HashSet<>(queries));

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
  public NegotiationDTO addRequestToNegotiation(String negotiationId, Request requestEntity) {
    Negotiation negotiationEntity = findEntityById(negotiationId, false);
    negotiationEntity.getRequests().add(requestEntity);
    requestEntity.setNegotiation(negotiationEntity);
    try {
      negotiationRepository.save(negotiationEntity);
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
   * Returns the Negotiation with the specified negotiationId if exists, otherwise it throws an exception
   *
   * @param negotiationId the negotiationId of the Negotiation to retrieve
   * @param includeDetails whether the negotiation returned include details
   * @return the Negotiation with specified negotiationId
   */
  @Transactional
  public NegotiationDTO findById(String negotiationId, boolean includeDetails) throws EntityNotFoundException {
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
}
