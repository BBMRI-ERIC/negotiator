package eu.bbmri_eric.negotiator.governance.resource;

import eu.bbmri_eric.negotiator.common.AuthenticatedUserContext;
import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.common.exceptions.ForbiddenRequestException;
import eu.bbmri_eric.negotiator.governance.network.Network;
import eu.bbmri_eric.negotiator.governance.network.NetworkRepository;
import eu.bbmri_eric.negotiator.governance.resource.dto.ResourceWithStatusDTO;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.negotiation.NewResourcesAddedEvent;
import eu.bbmri_eric.negotiator.negotiation.request.Request;
import eu.bbmri_eric.negotiator.negotiation.request.RequestRepository;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.NegotiationResourceState;
import eu.bbmri_eric.negotiator.notification.UserNotificationService;
import eu.bbmri_eric.negotiator.user.PersonRepository;
import eu.bbmri_eric.negotiator.user.ResourceResponseModel;
import jakarta.transaction.Transactional;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.NonNull;
import lombok.extern.apachecommons.CommonsLog;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@CommonsLog
public class ResourceServiceImpl implements ResourceService {

  private final NetworkRepository networkRepository;
  private final ResourceRepository repository;
  private final PersonRepository personRepository;
  private final NegotiationRepository negotiationRepository;
  private final ModelMapper modelMapper;
  private final RequestRepository requestRepository;
  private final ApplicationEventPublisher applicationEventPublisher;

  public ResourceServiceImpl(
      NetworkRepository networkRepository,
      ResourceRepository repository,
      PersonRepository personRepository,
      NegotiationRepository negotiationRepository,
      ModelMapper modelMapper,
      RequestRepository requestRepository,
      UserNotificationService userNotificationService,
      ApplicationEventPublisher applicationEventPublisher) {
    this.networkRepository = networkRepository;
    this.repository = repository;
    this.personRepository = personRepository;
    this.negotiationRepository = negotiationRepository;
    this.modelMapper = modelMapper;
    this.requestRepository = requestRepository;
    this.applicationEventPublisher = applicationEventPublisher;
  }

  @Override
  public ResourceResponseModel findById(Long id) {
    return modelMapper.map(
        repository.findById(id).orElseThrow(() -> new EntityNotFoundException(id)),
        ResourceResponseModel.class);
  }

  @Override
  public Iterable<ResourceResponseModel> findAll(Pageable pageable) {
    return repository
        .findAll(pageable)
        .map(resource -> modelMapper.map(resource, ResourceResponseModel.class));
  }

  @Override
  public Iterable<ResourceResponseModel> findAllForNetwork(Pageable pageable, Long networkId) {

    Network network =
        networkRepository
            .findById(networkId)
            .orElseThrow(() -> new EntityNotFoundException(networkId));
    return repository
        .findAllByNetworksContains(network, pageable)
        .map(resource -> modelMapper.map(resource, ResourceResponseModel.class));
  }

  @Override
  public List<ResourceWithStatusDTO> findAllInNegotiation(String negotiationId) {
    if (!negotiationRepository.existsById(negotiationId)) {
      throw new EntityNotFoundException(negotiationId);
    }
    Long userId = AuthenticatedUserContext.getCurrentlyAuthenticatedUserInternalId();
    if (userIsntAuthorized(negotiationId, userId)
        && !AuthenticatedUserContext.isCurrentlyAuthenticatedUserAdmin()) {
      throw new ForbiddenRequestException("You do not have permission to access this resource");
    }
    List<ResourceViewDTO> resourceViewDTOS = repository.findByNegotiation(negotiationId);
    return resourceViewDTOS.stream()
        .map(resourceViewDTO -> modelMapper.map(resourceViewDTO, ResourceWithStatusDTO.class))
        .toList();
  }

  @Override
  @Transactional
  public List<ResourceWithStatusDTO> addResourcesToNegotiation(
      String negotiationId, List<Long> resourceIds) {
    assignNewResources(negotiationId, resourceIds);
    return getResourceWithStatusDTOS(negotiationId);
  }

  private void assignNewResources(String negotiationId, List<Long> resourceIds) {
    Negotiation negotiation = getNegotiation(negotiationId);
    Set<Resource> resources = getResources(negotiationId, resourceIds, negotiation);
    initializeStateForNewResources(negotiation, resources);
    persistChanges(negotiation, resources);
  }

  private @NonNull List<ResourceWithStatusDTO> getResourceWithStatusDTOS(String negotiationId) {
    List<ResourceViewDTO> resourceViewDTOS = repository.findByNegotiation(negotiationId);
    log.debug(
        "Negotiation %s now has %s resources after modification"
            .formatted(negotiationId, resourceViewDTOS.size()));
    return resourceViewDTOS.stream()
        .map(resourceViewDTO -> modelMapper.map(resourceViewDTO, ResourceWithStatusDTO.class))
        .toList();
  }

  private void persistChanges(Negotiation negotiation, Set<Resource> resources) {
    Request request = negotiation.getRequests().iterator().next();
    negotiationRepository.saveAndFlush(negotiation);
    resources.addAll(negotiation.getResources());
    request.setResources(resources);
    requestRepository.saveAndFlush(request);
    if (negotiation.getCurrentState().equals(NegotiationState.IN_PROGRESS)) {
      applicationEventPublisher.publishEvent(new NewResourcesAddedEvent(this, negotiation.getId()));
    }
  }

  private static void initializeStateForNewResources(
      Negotiation negotiation, Set<Resource> resources) {
    if (negotiation.getCurrentState().equals(NegotiationState.IN_PROGRESS)) {
      for (Resource resource : resources) {
        negotiation.setStateForResource(resource.getSourceId(), NegotiationResourceState.SUBMITTED);
      }
    }
  }

  private @NonNull Set<Resource> getResources(
      String negotiationId, List<Long> resourceIds, Negotiation negotiation) {
    log.debug(
        "Negotiation %s has %s resources before modification"
            .formatted(negotiationId, negotiation.getResources().size()));
    Set<Resource> resources = new HashSet<>(repository.findAllById(resourceIds));
    log.debug(resources.size());
    resources.removeAll(negotiation.getResources());
    log.debug(
        "Request is to add %s new resources to negotiation %s"
            .formatted(resources.size(), negotiationId));
    return resources;
  }

  private Negotiation getNegotiation(String negotiationId) {
    Negotiation negotiation =
        negotiationRepository
            .findById(negotiationId)
            .orElseThrow(() -> new EntityNotFoundException(negotiationId));
    return negotiation;
  }

  private Request getRequest(String negotiationId) {
    Request request =
        requestRepository
            .findByNegotiation_Id(negotiationId)
            .orElseThrow(() -> new EntityNotFoundException(negotiationId));
    return request;
  }

  private boolean userIsntAuthorized(String negotiationId, Long userId) {
    return !personRepository.isRepresentativeOfAnyResourceOfNegotiation(userId, negotiationId)
        && !negotiationRepository.existsByIdAndCreatedBy_Id(negotiationId, userId);
  }
}
