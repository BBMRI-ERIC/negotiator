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
import eu.bbmri_eric.negotiator.governance.resource.dto.ResourceWithOrgDTO;
import eu.bbmri_eric.negotiator.governance.resource.dto.ResourceWithStatusDTO;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.NegotiationAccessManager;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.negotiation.NewResourcesAddedEvent;
import eu.bbmri_eric.negotiator.negotiation.dto.UpdateResourcesDTO;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.NegotiationResourceEvent;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.NegotiationResourceState;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.ResourceStateChangeEvent;
import eu.bbmri_eric.negotiator.user.Person;
import eu.bbmri_eric.negotiator.user.PersonRepository;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
  private final ApplicationEventPublisher applicationEventPublisher;
  private final AccessFormRepository accessFormRepository;
  private final DiscoveryServiceRepository discoveryServiceRepository;
  private final OrganizationRepository organizationRepository;
  private final NegotiationAccessManager negotiationAccessManager;

  public ResourceServiceImpl(
      NetworkRepository networkRepository,
      ResourceRepository repository,
      PersonRepository personRepository,
      NegotiationRepository negotiationRepository,
      ModelMapper modelMapper,
      ApplicationEventPublisher applicationEventPublisher,
      AccessFormRepository accessFormRepository,
      DiscoveryServiceRepository discoveryServiceRepository,
      OrganizationRepository organizationRepository,
      NegotiationAccessManager negotiationAccessManager) {
    this.networkRepository = networkRepository;
    this.repository = repository;
    this.personRepository = personRepository;
    this.negotiationRepository = negotiationRepository;
    this.modelMapper = modelMapper;
    this.applicationEventPublisher = applicationEventPublisher;
    this.accessFormRepository = accessFormRepository;
    this.discoveryServiceRepository = discoveryServiceRepository;
    this.organizationRepository = organizationRepository;
    this.negotiationAccessManager = negotiationAccessManager;
  }

  @Override
  public ResourceResponseModel findById(Long id) {
    return modelMapper.map(
        repository.findById(id).orElseThrow(() -> new EntityNotFoundException(id)),
        ResourceWithOrgDTO.class);
  }

  @Override
  public Iterable<ResourceResponseModel> findAll(FilterDTO filters) {
    Specification<Resource> spec = ResourceSpecificationBuilder.build(filters);
    Pageable pageable = PageRequest.of(filters.getPage(), filters.getSize());
    return repository
        .findAll(spec, pageable)
        .map(resource -> modelMapper.map(resource, ResourceWithOrgDTO.class));
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
  @Transactional
  public List<ResourceWithStatusDTO> findAllInNegotiation(String negotiationId) {
    if (!negotiationRepository.existsById(negotiationId)) {
      throw new EntityNotFoundException(negotiationId);
    }
    Long userId = AuthenticatedUserContext.getCurrentlyAuthenticatedUserInternalId();
    negotiationAccessManager.verifyReadAccessForNegotiation(negotiationId, userId);
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
    verifyAuthForStatusUpdate(resourcesToUpdate);
    resourcesToUpdate.forEach(resource -> updateResourceStatus(negotiation, state, resource));
  }

  private void updateResourceStatus(
      Negotiation negotiation, NegotiationResourceState state, Resource resource) {
    NegotiationResourceState before =
        negotiation.getCurrentStateForResource(resource.getSourceId());
    if (isUninitialized(state, before)) {
      negotiation.setStateForResource(resource.getSourceId(), state);
    } else if (isStateMachineInitialized(state)) {
      negotiation.setStateForResource(resource.getSourceId(), state);
      applicationEventPublisher.publishEvent(
          new ResourceStateChangeEvent(
              this,
              negotiation.getId(),
              resource.getSourceId(),
              before,
              state,
              NegotiationResourceEvent.OVERRIDE));
    }
  }

  private static boolean isStateMachineInitialized(NegotiationResourceState state) {
    return state != NegotiationResourceState.SUBMITTED;
  }

  private static boolean isUninitialized(
      NegotiationResourceState state, NegotiationResourceState before) {
    return state == NegotiationResourceState.SUBMITTED && before == null;
  }

  private void verifyAuthForStatusUpdate(Set<Resource> resourcesToUpdate) {
    Long userId = AuthenticatedUserContext.getCurrentlyAuthenticatedUserInternalId();
    Person representative =
        personRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException(userId));
    if (!AuthenticatedUserContext.isCurrentlyAuthenticatedUserAdmin()
        && !representative.getResources().containsAll(resourcesToUpdate)) {
      throw new ForbiddenRequestException("You do not have permission to update these resources");
    }
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
  @Transactional
  public ResourceResponseModel updateResourceById(Long id, ResourceUpdateDTO updateDTO) {
    Resource resource = repository.findById(id).orElseThrow(() -> new EntityNotFoundException(id));
    modelMapper.getConfiguration().setSkipNullEnabled(true);
    modelMapper.map(updateDTO, resource);
    resource = repository.saveAndFlush(resource);
    return modelMapper.map(resource, ResourceWithOrgDTO.class);
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
      Organization organization =
          organizationRepository
              .findById(resDTO.getOrganizationId())
              .orElseThrow(() -> new EntityNotFoundException(resDTO.getOrganizationId()));
      Resource res = modelMapper.map(resDTO, Resource.class);
      res.setOrganization(organization);
      res.setAccessForm(accessForm);
      res.setDiscoveryService(discoveryService);
      resources.add(res);
    }
    List<Resource> savedResources = repository.saveAll(resources);
    return savedResources.stream()
        .map(resource -> modelMapper.map(resource, ResourceResponseModel.class))
        .toList();
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

  private @NonNull Set<Resource> fetchResourcesFromDB(List<Long> resourceIds) {
    return new HashSet<>(repository.findAllById(resourceIds));
  }

  private Negotiation fetchNegotiationFromDB(String negotiationId) {
    return negotiationRepository
        .findById(negotiationId)
        .orElseThrow(() -> new EntityNotFoundException(negotiationId));
  }
}
