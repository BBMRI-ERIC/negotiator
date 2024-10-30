package eu.bbmri_eric.negotiator.governance.resource;

import eu.bbmri_eric.negotiator.common.AuthenticatedUserContext;
import eu.bbmri_eric.negotiator.common.FilterDTO;
import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.common.exceptions.ForbiddenRequestException;
import eu.bbmri_eric.negotiator.discovery.DiscoveryService;
import eu.bbmri_eric.negotiator.discovery.DiscoveryServiceRepository;
import eu.bbmri_eric.negotiator.discovery.synchronization.DiscoveryServiceNotFoundException;
import eu.bbmri_eric.negotiator.form.AccessForm;
import eu.bbmri_eric.negotiator.form.AccessFormNotFoundException;
import eu.bbmri_eric.negotiator.form.repository.AccessFormRepository;
import eu.bbmri_eric.negotiator.governance.network.Network;
import eu.bbmri_eric.negotiator.governance.network.NetworkRepository;
import eu.bbmri_eric.negotiator.governance.organization.Organization;
import eu.bbmri_eric.negotiator.governance.organization.OrganizationRepository;
import eu.bbmri_eric.negotiator.governance.resource.dto.ResourceCreateDTO;
import eu.bbmri_eric.negotiator.governance.resource.dto.ResourceResponseModel;
import eu.bbmri_eric.negotiator.governance.resource.dto.ResourceUpdateDTO;
import eu.bbmri_eric.negotiator.governance.resource.dto.ResourceWithStatusDTO;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.negotiation.NewResourcesAddedEvent;
import eu.bbmri_eric.negotiator.negotiation.dto.UpdateResourcesDTO;
import eu.bbmri_eric.negotiator.negotiation.request.RequestRepository;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.NegotiationResourceState;
import eu.bbmri_eric.negotiator.notification.UserNotificationService;
import eu.bbmri_eric.negotiator.user.Person;
import eu.bbmri_eric.negotiator.user.PersonRepository;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.extern.apachecommons.CommonsLog;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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
  private final AccessFormRepository accessFormRepository;
  private final DiscoveryServiceRepository discoveryServiceRepository;
  private final OrganizationRepository organizationRepository;

  public ResourceServiceImpl(
      NetworkRepository networkRepository,
      ResourceRepository repository,
      PersonRepository personRepository,
      NegotiationRepository negotiationRepository,
      ModelMapper modelMapper,
      RequestRepository requestRepository,
      UserNotificationService userNotificationService,
      ApplicationEventPublisher applicationEventPublisher,
      AccessFormRepository accessFormRepository,
      DiscoveryServiceRepository discoveryServiceRepository,
      OrganizationRepository organizationRepository) {
    this.networkRepository = networkRepository;
    this.repository = repository;
    this.personRepository = personRepository;
    this.negotiationRepository = negotiationRepository;
    this.modelMapper = modelMapper;
    this.requestRepository = requestRepository;
    this.applicationEventPublisher = applicationEventPublisher;
    this.accessFormRepository = accessFormRepository;
    this.discoveryServiceRepository = discoveryServiceRepository;
    this.organizationRepository = organizationRepository;
  }

  @Override
  public ResourceResponseModel findById(Long id) {
    return modelMapper.map(
        repository.findById(id).orElseThrow(() -> new EntityNotFoundException(id)),
        ResourceResponseModel.class);
  }

  @Override
  public Iterable<ResourceResponseModel> findAll(FilterDTO filters) {
    Specification<Resource> spec = ResourceSpecificationBuilder.build(filters);
    Pageable pageable = PageRequest.of(filters.getPage(), filters.getSize());
    return repository
        .findAll(spec, pageable)
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
  public List<ResourceWithStatusDTO> updateResourcesInANegotiation(
      String negotiationId, UpdateResourcesDTO updateResourcesDTO) {
    Negotiation negotiation = fetchNegotiationFromDB(negotiationId);
    Set<Resource> resourcesToUpdate = fetchResourcesFromDB(updateResourcesDTO.getResourceIds());
    addAnyNewResourcesToNegotiation(resourcesToUpdate, negotiation);
    setStatusForUpdatedResources(negotiation, resourcesToUpdate, updateResourcesDTO.getState());
    negotiationRepository.saveAndFlush(negotiation);
    if (negotiation.getCurrentState().equals(NegotiationState.IN_PROGRESS)) {
      applicationEventPublisher.publishEvent(new NewResourcesAddedEvent(this, negotiation.getId()));
    }
    return getResourceWithStatusDTOS(negotiationId);
  }

  private void setStatusForUpdatedResources(
      Negotiation negotiation, Set<Resource> resourcesToUpdate, NegotiationResourceState state) {

    Long userId = AuthenticatedUserContext.getCurrentlyAuthenticatedUserInternalId();
    Person representative =
        personRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException(userId));

    // Check permissions if the user is not an admin
    if (!AuthenticatedUserContext.isCurrentlyAuthenticatedUserAdmin()
        && !representative.getResources().containsAll(resourcesToUpdate)) {
      throw new ForbiddenRequestException("You do not have permission to update these resources");
    }

    // Update state for each resource, conditionally handling 'SUBMITTED' state
    resourcesToUpdate.forEach(
        resource -> {
          if (state == NegotiationResourceState.SUBMITTED
              && negotiation.getCurrentStateForResource(resource.getSourceId()) == null) {
            negotiation.setStateForResource(resource.getSourceId(), state);
          } else if (state != NegotiationResourceState.SUBMITTED) {
            negotiation.setStateForResource(resource.getSourceId(), state);
          }
        });
  }

  private static void addAnyNewResourcesToNegotiation(
      Set<Resource> resourcesToUpdate, Negotiation negotiation) {
    if (!negotiation.getResources().containsAll(resourcesToUpdate)) {
      if (!AuthenticatedUserContext.isCurrentlyAuthenticatedUserAdmin()) {
        throw new ForbiddenRequestException(
            "You do not have permission to add resources to this negotiation");
      }
      Set<Resource> newNegotiationResources = new HashSet<>(negotiation.getResources());
      newNegotiationResources.addAll(resourcesToUpdate);
      negotiation.setResources(newNegotiationResources);
    }
  }

  @Override
  public ResourceResponseModel updateResourceById(Long id, ResourceUpdateDTO resource) {
    Resource res = repository.findById(id).orElseThrow(() -> new EntityNotFoundException(id));
    res.setName(resource.getName());
    res.setDescription(resource.getDescription());
    Resource updatedResource = repository.save(res);
    return modelMapper.map(updatedResource, ResourceResponseModel.class);
  }

  @Override
  @Transactional
  public List<ResourceResponseModel> addResources(List<ResourceCreateDTO> resourcesCreateDTO) {
    ArrayList<Resource> resources = new ArrayList<>();
    for (ResourceCreateDTO resDTO : resourcesCreateDTO) {
      DiscoveryService discoveryService =
          discoveryServiceRepository
              .findById(resDTO.getDiscoveryServiceId())
              .orElseThrow(
                  () -> new DiscoveryServiceNotFoundException(resDTO.getDiscoveryServiceId()));
      AccessForm accessForm =
          accessFormRepository
              .findById(resDTO.getAccessFormId())
              .orElseThrow(() -> new AccessFormNotFoundException(resDTO.getAccessFormId()));
      Optional<Organization> organization =
          organizationRepository.findById(resDTO.getOrganizationId());
      Resource res =
          Resource.builder()
              .name(resDTO.getName())
              .description(resDTO.getDescription())
              .sourceId(resDTO.getSourceId())
              .organization(organization.get())
              .discoveryService(discoveryService)
              .accessForm(accessForm)
              .build();
      resources.add(res);
    }
    List<Resource> savedResources = repository.saveAll(resources);
    return savedResources.stream()
        .map(resource -> modelMapper.map(resource, ResourceResponseModel.class))
        .collect(Collectors.toList());
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

  private void persistChanges(
      Negotiation negotiation, Set<Resource> resources, NegotiationResourceState state) {}

  private static void initializeStateForNewResources(
      Negotiation negotiation, Set<Resource> resources, NegotiationResourceState state) {
    if (negotiation.getCurrentState().equals(NegotiationState.IN_PROGRESS)) {
      for (Resource resource : resources) {
        negotiation.setStateForResource(resource.getSourceId(), state);
      }
    }
  }

  private @NonNull Set<Resource> fetchResourcesFromDB(List<Long> resourceIds) {
    return new HashSet<>(repository.findAllById(resourceIds));
  }

  private Negotiation fetchNegotiationFromDB(String negotiationId) {
    return negotiationRepository
        .findById(negotiationId)
        .orElseThrow(() -> new EntityNotFoundException(negotiationId));
  }

  private boolean userIsntAuthorized(String negotiationId, Long userId) {
    return !personRepository.isRepresentativeOfAnyResourceOfNegotiation(userId, negotiationId)
        && !negotiationRepository.existsByIdAndCreatedBy_Id(negotiationId, userId);
  }
}
