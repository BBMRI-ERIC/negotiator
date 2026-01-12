package eu.bbmri_eric.negotiator.negotiation;

import eu.bbmri_eric.negotiator.attachment.Attachment;
import eu.bbmri_eric.negotiator.attachment.AttachmentRepository;
import eu.bbmri_eric.negotiator.attachment.dto.AttachmentMetadataDTO;
import eu.bbmri_eric.negotiator.common.AuthenticatedUserContext;
import eu.bbmri_eric.negotiator.common.exceptions.ConflictStatusException;
import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.common.exceptions.EntityNotStorableException;
import eu.bbmri_eric.negotiator.common.exceptions.ForbiddenRequestException;
import eu.bbmri_eric.negotiator.common.exceptions.WrongRequestException;
import eu.bbmri_eric.negotiator.governance.network.Network;
import eu.bbmri_eric.negotiator.governance.network.NetworkRepository;
import eu.bbmri_eric.negotiator.negotiation.dto.NegotiationCreateDTO;
import eu.bbmri_eric.negotiator.negotiation.dto.NegotiationDTO;
import eu.bbmri_eric.negotiator.negotiation.dto.NegotiationFilterDTO;
import eu.bbmri_eric.negotiator.negotiation.dto.NegotiationUpdateDTO;
import eu.bbmri_eric.negotiator.negotiation.request.Request;
import eu.bbmri_eric.negotiator.negotiation.request.RequestRepository;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.user.Person;
import eu.bbmri_eric.negotiator.user.PersonRepository;
import eu.bbmri_eric.negotiator.user.PersonService;
import jakarta.transaction.Transactional;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.apachecommons.CommonsLog;
import org.hibernate.exception.DataException;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

@Service(value = "DefaultNegotiationService")
@CommonsLog
@Transactional
public class NegotiationServiceImpl implements NegotiationService {

  private NegotiationRepository negotiationRepository;
  private PersonRepository personRepository;
  private RequestRepository requestRepository;
  private AttachmentRepository attachmentRepository;
  private NetworkRepository networkRepository;
  private ModelMapper modelMapper;
  private PersonService personService;
  private ApplicationEventPublisher eventPublisher;
  private NegotiationAccessManager negotiationAccessManager;

  public NegotiationServiceImpl(
      NegotiationRepository negotiationRepository,
      PersonRepository personRepository,
      RequestRepository requestRepository,
      AttachmentRepository attachmentRepository,
      NetworkRepository networkRepository,
      ModelMapper modelMapper,
      PersonService personService,
      ApplicationEventPublisher eventPublisher,
      NegotiationAccessManager negotiationAccessManager) {
    this.negotiationRepository = negotiationRepository;
    this.personRepository = personRepository;
    this.requestRepository = requestRepository;
    this.attachmentRepository = attachmentRepository;
    this.networkRepository = networkRepository;
    this.modelMapper = modelMapper;
    this.personService = personService;
    this.eventPublisher = eventPublisher;
    this.negotiationAccessManager = negotiationAccessManager;
  }

  @Override
  public boolean isNegotiationCreator(String negotiationId) {
    return personRepository.isNegotiationCreator(
        AuthenticatedUserContext.getCurrentlyAuthenticatedUserInternalId(), negotiationId);
  }

  /**
   * Check if the currently authenticated user is authorized for teh negotiation
   *
   * @param negotiationId the id of the negotiaton to check
   * @return true if the user is the creator of the negotiation or if he/she is representative of
   *     any resource involved in the negotiation
   */
  @Override
  public boolean isAuthorizedForNegotiation(String negotiationId) {
    return isNegotiationCreator(negotiationId)
        || personService.isRepresentativeOfAnyResourceOfNegotiation(
            AuthenticatedUserContext.getCurrentlyAuthenticatedUserInternalId(), negotiationId)
        || AuthenticatedUserContext.isCurrentlyAuthenticatedUserAdmin();
  }

  public boolean isOrganizationPartOfNegotiation(
      String negotiationId, String organizationExternalId) {
    return negotiationRepository.isOrganizationPartOfNegotiation(
        negotiationId, organizationExternalId);
  }

  private Request getRequest(String requestId) {
    return requestRepository
        .findById(requestId)
        .orElseThrow(
            () -> new WrongRequestException("One or more of the specified requests do not exist"));
  }

  private List<Attachment> findAttachments(Set<AttachmentMetadataDTO> attachmentDTOs) {
    List<Attachment> entities =
        attachmentRepository.findAllById(
            attachmentDTOs.stream().map(AttachmentMetadataDTO::getId).collect(Collectors.toList()));
    if (entities.size() < attachmentDTOs.size()) {
      throw new WrongRequestException("One or more of the specified attachments do not exist");
    }
    return entities;
  }

  @Override
  public boolean exists(String negotiationId) {
    return negotiationRepository.existsById(negotiationId);
  }

  /**
   * Creates a Negotiation.
   *
   * @param negotiationBody the NegotiationCreateDTO DTO sent from to the endpoint
   * @param creatorId the ID of the Person that creates the Negotiation (i.e., the authenticated
   *     Person that called the API)
   * @return the created Negotiation entity
   */
  @Transactional
  public NegotiationDTO create(NegotiationCreateDTO negotiationBody, Long creatorId) {
    Request request = getRequest(negotiationBody.getRequest());
    Negotiation negotiation = buildNegotiation(negotiationBody, request);
    negotiation = persistNegotiation(negotiationBody, negotiation);
    requestRepository.delete(request);
    return modelMapper.map(negotiation, NegotiationDTO.class);
  }

  private Negotiation persistNegotiation(
      NegotiationCreateDTO negotiationBody, Negotiation negotiation) {
    try {
      negotiation = negotiationRepository.save(negotiation);
    } catch (DataException | DataIntegrityViolationException ex) {
      log.error("Error while saving the Negotiation into db. Some db constraint violated", ex);
      throw new EntityNotStorableException();
    }
    if (negotiationBody.getAttachments() != null) {
      linkAttachments(negotiationBody, negotiation);
    }
    if (Objects.equals(negotiation.getCurrentState(), NegotiationState.SUBMITTED)) {
      eventPublisher.publishEvent(new NewNegotiationEvent(this, negotiation.getId()));
    }
    return negotiation;
  }

  private void linkAttachments(NegotiationCreateDTO negotiationBody, Negotiation negotiation) {
    List<Attachment> attachments = findAttachments(negotiationBody.getAttachments());
    negotiation.setAttachments(new HashSet<>(attachments));
  }

  private Negotiation buildNegotiation(NegotiationCreateDTO negotiationBody, Request request) {
    Negotiation negotiation = modelMapper.map(negotiationBody, Negotiation.class);
    negotiation.setResources(new HashSet<>(request.getResources()));
    negotiation.setHumanReadable(request.getHumanReadable());
    negotiation.setDiscoveryService(request.getDiscoveryService());
    return negotiation;
  }

  /**
   * Updates the negotiation with the specified ID.
   *
   * @param negotiationId the negotiationId of the negotiation tu update
   * @param updateDTO the NegotiationCreateDTO DTO with the new Negotiation data
   * @return The updated Negotiation entity
   */
  public NegotiationDTO update(String negotiationId, NegotiationUpdateDTO updateDTO) {
    Negotiation negotiationEntity = findEntityById(negotiationId, true);
    verifyWriteAccessToNegotiation(negotiationEntity);
    if (Objects.nonNull(updateDTO.getPayload())) {
      negotiationEntity.setPayload(updateDTO.getPayload().toString());
    }
    if (Objects.nonNull(updateDTO.getDisplayId()) && AuthenticatedUserContext.isCurrentlyAuthenticatedUserAdmin()) {
      negotiationEntity.setDisplayId(updateDTO.getDisplayId());
    }
    if (Objects.nonNull(updateDTO.getAuthorSubjectId())) {
      log.info("Transferring Negotiation");
      Person person =
          personRepository
              .findBySubjectId(updateDTO.getAuthorSubjectId())
              .orElseThrow(() -> new EntityNotFoundException(updateDTO.getAuthorSubjectId()));
      negotiationEntity.setCreatedBy(person);
    }
    negotiationRepository.saveAndFlush(negotiationEntity);
    return modelMapper.map(negotiationEntity, NegotiationDTO.class);
  }

  private static void verifyWriteAccessToNegotiation(Negotiation negotiationEntity) {
    if (!AuthenticatedUserContext.getCurrentlyAuthenticatedUserInternalId()
            .equals(negotiationEntity.getCreatedBy().getId())
        && !AuthenticatedUserContext.isCurrentlyAuthenticatedUserAdmin()) {
      throw new ForbiddenRequestException("You are not allowed to update this entity");
    }
  }

  /**
   * Returns all negotiation in the repository
   *
   * @return the List of Negotiation entities
   */
  public Iterable<NegotiationDTO> findAll(Pageable pageable) {
    return negotiationRepository
        .findAll(pageable)
        .map(negotiation -> modelMapper.map(negotiation, NegotiationDTO.class));
  }

  @Override
  public Iterable<NegotiationDTO> findAllByCurrentStatus(
      Pageable pageable, NegotiationState state) {
    return negotiationRepository
        .findAll(NegotiationSpecification.hasState(List.of(state), false), pageable)
        .map(negotiation -> modelMapper.map(negotiation, NegotiationDTO.class));
  }

  private Iterable<NegotiationDTO> performQueryByFilters(
      NegotiationFilterDTO filtersDTO, @Nullable Person user) {
    Specification<Negotiation> filtersSpec =
        NegotiationSpecification.fromNegotiationFilters(filtersDTO, user, null);

    Pageable pageable =
        PageRequest.of(
            filtersDTO.getPage(),
            filtersDTO.getSize(),
            Sort.by(filtersDTO.getSortOrder(), filtersDTO.getSortBy().name()));

    return negotiationRepository
        .findAll(filtersSpec, pageable)
        .map(negotiation -> modelMapper.map(negotiation, NegotiationDTO.class));
  }

  /**
   * Method to filter negotiations. It dynamically creates query conditions depending on the
   * NegotiationFilterDTO in input and returns the filtered negotiations
   *
   * @param filtersDTO a NegotiationFilterDTO object containing the filter parameters
   * @return an Iterable of NegotiationDTO with the filtered Negotiations
   */
  @Override
  public Iterable<NegotiationDTO> findAllByFilters(NegotiationFilterDTO filtersDTO) {
    return performQueryByFilters(filtersDTO, null);
  }

  /**
   * Method to filter negotiations. It dynamically creates query conditions depending on the
   * NegotiationFilterDTI in input and returns the filtered negotiations
   *
   * @param filtersDTO a NegotiationFilterDTO object containing the filter parameters
   * @param userId the id of the user that is performing the action
   * @return an Iterable of NegotiationDTO with the filtered Negotiations
   */
  @Override
  public Iterable<NegotiationDTO> findByFiltersForUser(
      NegotiationFilterDTO filtersDTO, Long userId) {
    Person user =
        personRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException(userId));

    return performQueryByFilters(filtersDTO, user);
  }

  @Override
  public Iterable<NegotiationDTO> findAllForNetwork(
      Long networkId, NegotiationFilterDTO filtersDTO) {
    if (!personRepository.isNetworkManager(
        AuthenticatedUserContext.getCurrentlyAuthenticatedUserInternalId(), networkId)) {
      throw new ForbiddenRequestException("You are not allowed to perform this operation");
    }
    Network network =
        networkRepository
            .findById(networkId)
            .orElseThrow(() -> new EntityNotFoundException(networkId));

    Specification<Negotiation> filtersSpec =
        NegotiationSpecification.fromNegotiationFilters(filtersDTO, null, network);

    Pageable pageable =
        PageRequest.of(
            filtersDTO.getPage(),
            filtersDTO.getSize(),
            Sort.by(filtersDTO.getSortOrder(), filtersDTO.getSortBy().name()));
    return negotiationRepository
        .findAll(filtersSpec, pageable)
        .map(negotiation -> modelMapper.map(negotiation, NegotiationDTO.class));
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
  @Override
  public NegotiationDTO findById(String negotiationId, boolean includeDetails)
      throws EntityNotFoundException {
    if (!negotiationRepository.existsById(negotiationId)) {
      throw new EntityNotFoundException(negotiationId);
    }
    Long userID = AuthenticatedUserContext.getCurrentlyAuthenticatedUserInternalId();
    negotiationAccessManager.verifyReadAccessForNegotiation(negotiationId, userID);
    Negotiation negotiation = findEntityById(negotiationId, includeDetails);
    return modelMapper.map(negotiation, NegotiationDTO.class);
  }

  public void setPrivatePostsEnabled(String negotiationId, boolean enabled) {
    Negotiation negotiation =
        negotiationRepository
            .findById(negotiationId)
            .orElseThrow(() -> new EntityNotFoundException(negotiationId));
    negotiation.setPrivatePostsEnabled(enabled);
  }

  public void setPublicPostsEnabled(String negotiationId, boolean enabled) {
    Negotiation negotiation =
        negotiationRepository
            .findById(negotiationId)
            .orElseThrow(() -> new EntityNotFoundException(negotiationId));
    negotiation.setPublicPostsEnabled(enabled);
  }

  @Override
  public List<NegotiationDTO> findAllWithCurrentState(NegotiationState negotiationState) {
    return negotiationRepository
        .findAll(NegotiationSpecification.hasState(List.of(negotiationState), false))
        .stream()
        .map(negotiation -> modelMapper.map(negotiation, NegotiationDTO.class))
        .collect(Collectors.toList());
  }

  public void deleteNegotiation(String negotiationId) {
    Negotiation negotiation = findEntityById(negotiationId, false);
    if (!isNegotiationCreator(negotiationId)
        && !AuthenticatedUserContext.isCurrentlyAuthenticatedUserAdmin()) {
      throw new ForbiddenRequestException("You are not allowed to delete this entity");
    }
    if (negotiation.getCurrentState() != NegotiationState.DRAFT) {
      throw new ConflictStatusException("Cannot delete a Negotiation that is not in DRAFT state");
    }
    negotiationRepository.delete(negotiation);
  }
}
