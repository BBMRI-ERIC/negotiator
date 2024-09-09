package eu.bbmri_eric.negotiator.governance.resource;

import eu.bbmri_eric.negotiator.common.AuthenticatedUserContext;
import eu.bbmri_eric.negotiator.common.FilterDTO;
import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.common.exceptions.ForbiddenRequestException;
import eu.bbmri_eric.negotiator.discovery.DiscoveryService;
import eu.bbmri_eric.negotiator.discovery.DiscoveryServiceRepository;
import eu.bbmri_eric.negotiator.form.AccessForm;
import eu.bbmri_eric.negotiator.form.repository.AccessFormRepository;
import eu.bbmri_eric.negotiator.governance.network.Network;
import eu.bbmri_eric.negotiator.governance.network.NetworkRepository;
import eu.bbmri_eric.negotiator.governance.organization.Organization;
import eu.bbmri_eric.negotiator.governance.organization.OrganizationRepository;
import eu.bbmri_eric.negotiator.governance.resource.dto.ResourceCreateDTO;
import eu.bbmri_eric.negotiator.governance.resource.dto.ResourceDTO;
import eu.bbmri_eric.negotiator.governance.resource.dto.ResourceResponseModel;
import eu.bbmri_eric.negotiator.governance.resource.dto.ResourceWithStatusDTO;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.negotiation.NewResourcesAddedEvent;
import eu.bbmri_eric.negotiator.negotiation.dto.UpdateResourcesDTO;
import eu.bbmri_eric.negotiator.negotiation.request.RequestRepository;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.NegotiationResourceState;
import eu.bbmri_eric.negotiator.notification.UserNotificationService;
import eu.bbmri_eric.negotiator.user.PersonRepository;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
    updateResources(negotiationId, updateResourcesDTO);
    return getResourceWithStatusDTOS(negotiationId);
  }

  @Override
  @Transactional
  public List<ResourceDTO> addResources(List<ResourceCreateDTO> resourcesCreateDTO) {
    ArrayList<Resource> resources = new ArrayList<Resource>();
    DiscoveryService discoveryService =
        discoveryServiceRepository
            .findById(Long.valueOf("1"))
            .orElseThrow(() -> new EntityNotFoundException("1"));
    AccessForm accessForm =
        accessFormRepository
            .findById(Long.valueOf("1"))
            .orElseThrow(() -> new EntityNotFoundException("1"));

    for (ResourceCreateDTO resDTO : resourcesCreateDTO) {
      Optional<Organization> organization =
          organizationRepository.findByExternalId(resDTO.getOrganizationId());
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
    return ResourceModelMapper.toDtoList(savedResources);
  }

  private void updateResources(String negotiationId, UpdateResourcesDTO updateResourcesDTO) {
    Negotiation negotiation = getNegotiation(negotiationId);
    Set<Resource> resources = getResources(negotiationId, updateResourcesDTO, negotiation);
    persistChanges(negotiation, resources, updateResourcesDTO.getState());
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
      Negotiation negotiation, Set<Resource> resources, NegotiationResourceState state) {
    resources.addAll(negotiation.getResources());
    negotiation.setResources(resources);
    initializeStateForNewResources(negotiation, resources, state);
    negotiationRepository.saveAndFlush(negotiation);
    if (negotiation.getCurrentState().equals(NegotiationState.IN_PROGRESS)) {
      applicationEventPublisher.publishEvent(new NewResourcesAddedEvent(this, negotiation.getId()));
    }
  }

  private static void initializeStateForNewResources(
      Negotiation negotiation, Set<Resource> resources, NegotiationResourceState state) {
    if (negotiation.getCurrentState().equals(NegotiationState.IN_PROGRESS)) {
      for (Resource resource : resources) {
        negotiation.setStateForResource(resource.getSourceId(), state);
      }
    }
  }

  private @NonNull Set<Resource> getResources(
      String negotiationId, UpdateResourcesDTO updateResourcesDTO, Negotiation negotiation) {
    log.debug(
        "Negotiation %s has %s resources before modification"
            .formatted(negotiationId, negotiation.getResources().size()));
    Set<Resource> resources =
        new HashSet<>(repository.findAllById(updateResourcesDTO.getResourceIds()));
    if (updateResourcesDTO.getState().equals(NegotiationResourceState.SUBMITTED)) {
      resources.removeAll(negotiation.getResources());
    }
    log.debug(
        "Request is to add %s new resources to negotiation %s"
            .formatted(resources.size(), negotiationId));
    return resources;
  }

  private Negotiation getNegotiation(String negotiationId) {
    return negotiationRepository
        .findById(negotiationId)
        .orElseThrow(() -> new EntityNotFoundException(negotiationId));
  }

  private boolean userIsntAuthorized(String negotiationId, Long userId) {
    return !personRepository.isRepresentativeOfAnyResourceOfNegotiation(userId, negotiationId)
        && !negotiationRepository.existsByIdAndCreatedBy_Id(negotiationId, userId);
  }
}
