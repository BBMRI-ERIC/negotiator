package eu.bbmri_eric.negotiator.user;

import eu.bbmri_eric.negotiator.common.AuthenticatedUserContext;
import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.common.exceptions.UserNotFoundException;
import eu.bbmri_eric.negotiator.governance.network.Network;
import eu.bbmri_eric.negotiator.governance.network.NetworkRepository;
import eu.bbmri_eric.negotiator.governance.organization.*;
import eu.bbmri_eric.negotiator.governance.resource.Resource;
import eu.bbmri_eric.negotiator.governance.resource.ResourceRepository;
import eu.bbmri_eric.negotiator.governance.resource.dto.ResourceResponseModel;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.notification.NotificationCreateDTO;
import eu.bbmri_eric.negotiator.notification.NotificationService;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.apachecommons.CommonsLog;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
@Transactional
@CommonsLog
public class PersonServiceImpl implements PersonService {

  private final NetworkRepository networkRepository;

  private final PersonRepository personRepository;

  private final ResourceRepository resourceRepository;
  private final OrganizationRepository organizationRepository;

  private final NegotiationRepository negotiationRepository;

  private final ModelMapper modelMapper;
  private final ApplicationEventPublisher eventPublisher;

  private final NotificationService notificationService;

  public PersonServiceImpl(
      NetworkRepository networkRepository,
      PersonRepository personRepository,
      ResourceRepository resourceRepository,
      OrganizationRepository organizationRepository,
      NegotiationRepository negotiationRepository,
      ModelMapper modelMapper,
      ApplicationEventPublisher eventPublisher,
      NotificationService notificationService) {
    this.networkRepository = networkRepository;
    this.personRepository = personRepository;
    this.resourceRepository = resourceRepository;
    this.organizationRepository = organizationRepository;
    this.negotiationRepository = negotiationRepository;
    this.modelMapper = modelMapper;
    this.eventPublisher = eventPublisher;
    this.notificationService = notificationService;
  }

  public UserResponseModel findById(Long id) {
    return modelMapper.map(getRepresentative(id), UserResponseModel.class);
  }

  @Override
  public Iterable<UserResponseModel> findAllByFilters(UserFilterDTO filtersDTO) {
    if (filtersDTO.getPage() < 0)
      throw new IllegalArgumentException("Page must be greater than 0.");
    if (filtersDTO.getSize() < 1)
      throw new IllegalArgumentException("Size must be greater than 0.");
    Specification<Person> filtersSpec = PersonSpecifications.fromUserFilters(filtersDTO);
    Pageable pageable =
        PageRequest.of(
            filtersDTO.getPage(),
            filtersDTO.getSize(),
            Sort.by(
                new Sort.Order(filtersDTO.getSortOrder(), filtersDTO.getSortBy().name())
                    .ignoreCase()));

    Page<UserResponseModel> result =
        personRepository
            .findAll(filtersSpec, pageable)
            .map(person -> modelMapper.map(person, UserResponseModel.class));

    if (filtersDTO.getPage() > result.getTotalPages())
      throw new IllegalArgumentException(
          "For the given size the page must be less than/equal to " + result.getTotalPages() + ".");
    return result;
  }

  @Override
  public List<UserResponseModel> findAllByOrganizationId(Long id) {
    return personRepository.findAllByOrganizationId(id).stream()
        .map(person -> modelMapper.map(person, UserResponseModel.class))
        .toList();
  }

  @Override
  public boolean isRepresentativeOfAnyResource(Long personId, List<String> resourceExternalIds) {
    Person person =
        personRepository
            .findById(personId)
            .orElseThrow(
                () -> new EntityNotFoundException("Person with id " + personId + " not found"));
    return person.getResources().stream()
        .anyMatch(resource -> resourceExternalIds.contains(resource.getSourceId()));
  }

  @Override
  public boolean isRepresentativeOfAnyResourceOfOrganization(Long personId, Long organizationId) {
    return personRepository.isRepresentativeOfAnyResourceOfOrganization(personId, organizationId);
  }

  @Override
  public boolean isRepresentativeOfAnyResourceOfOrganization(
      Long personId, String organizationExternalId) {
    return personRepository.isRepresentativeOfAnyResourceOfOrganization(
        personId, organizationExternalId);
  }

  @Override
  public boolean isRepresentativeOfAnyResourceOfNegotiation(Long personId, String negotiationId) {
    return personRepository.isRepresentativeOfAnyResourceOfNegotiation(personId, negotiationId);
  }

  @Override
  public Set<ResourceResponseModel> getResourcesRepresentedByUserId(Long personId) {
    AuthenticatedUserContext.checkUserAccess(personId);
    Set<Resource> resources =
        personRepository
            .findDetailedById(personId)
            .orElseThrow(() -> new UserNotFoundException(personId))
            .getResources();
    return resources.stream()
        .map(resource -> modelMapper.map(resource, ResourceResponseModel.class))
        .collect(Collectors.toSet());
  }

  @Override
  public void assignAsRepresentativeForResource(Long representativeId, Long resourceId) {
    Person representative = getRepresentative(representativeId);
    Resource resource = getResource(resourceId);
    log.warn(
        "AUTH_CHANGE: %s added as a representative for resource: %s"
            .formatted(representative.getName(), resource.getSourceId()));
    if (resource.getRepresentatives().isEmpty()) {
      eventPublisher.publishEvent(
          new FirstRepresentativeEvent(this, resource.getId(), resource.getSourceId()));
    }
    representative.addResource(resource);
    personRepository.save(representative);
    log.warn("Getting all negotiations where the representative might be involved...");

    log.warn(
        "Notifying user "
            + representativeId
            + " for negotiations involving resource "
            + resourceId);
    String title = "You have been added as a representative for a resource";
    String body =
        "You have been added as a representative for the resource:<br/><br/>"
            + " - Resource Name: "
            + resource.getName()
            + ".<br/><br/>"
            + " - Resource ID: "
            + resource.getSourceId()
            + ".<br/>"
            + "Please log in to the BBMRI Negotiator to review all the ongoing negotiations involving this resource.";
    NotificationCreateDTO notification =
        new NotificationCreateDTO(
            new ArrayList<>(Collections.singletonList(representativeId)), title, body);
    notificationService.createNotifications(notification);
  }

  @Override
  public void removeAsRepresentativeForResource(Long representativeId, Long resourceId) {
    Person representative = getRepresentative(representativeId);
    Resource resource = getResource(resourceId);
    log.warn(
        "AUTH_CHANGE: %s removed as a representative for resource: %s"
            .formatted(representative.getName(), resource.getSourceId()));
    representative.removeResource(resource);
    personRepository.save(representative);
  }

  @Override
  public Iterable<UserResponseModel> findAllForNetwork(Pageable pageable, Long networkId) {
    Network network =
        networkRepository
            .findById(networkId)
            .orElseThrow(() -> new EntityNotFoundException(networkId));
    return personRepository
        .findAllByNetworksContains(network, pageable)
        .map(person -> modelMapper.map(person, UserResponseModel.class));
  }

  @Override
  public void assignAsManagerForNetwork(Long managerId, Long networkId) {
    Person manager = getRepresentative(managerId);
    Network network = getNetwork(networkId);
    log.warn(
        "AUTH_CHANGE: %s added as a manager for network: %s"
            .formatted(manager.getName(), network.getName()));
    network.addManager(manager);
    networkRepository.save(network);
  }

  @Override
  public void removeAsManagerForNetwork(Long managerId, Long networkId) {
    Person manager = getRepresentative(managerId);
    Network network = getNetwork(networkId);
    log.warn(
        "AUTH_CHANGE: %s removed as a manager for network: %s"
            .formatted(manager.getName(), network.getName()));
    network.removeManager(manager);
    networkRepository.save(network);
  }

  @Override
  public Set<OrganizationDTO> getOrganizationsContainingResourceRepresentedByUser(
      Long personId, String expand, String name, Boolean withdrawn) {
    AuthenticatedUserContext.checkUserAccess(personId);
    Set<Organization> organizations =
        organizationRepository.findAllOrganizationsContainingResourceRepresentedByUser(
            personId, name, withdrawn);
    return organizations.stream()
        .map(
            o -> {
              if (Objects.equals(expand, "resources")) {
                OrganizationWithResourcesDTO dto =
                    modelMapper.map(o, OrganizationWithResourcesDTO.class);
                dto.setResources(
                    resourceRepository
                        .findByRepresentativeAndOrganization(personId, o.getId())
                        .stream()
                        .map(resource -> modelMapper.map(resource, ResourceResponseModel.class))
                        .collect(Collectors.toSet()));
                return dto;
              } else {
                return modelMapper.map(o, OrganizationDTO.class);
              }
            })
        .collect(Collectors.toSet());
  }

  private Resource getResource(Long resourceId) {
    return resourceRepository
        .findById(resourceId)
        .orElseThrow(() -> new EntityNotFoundException(resourceId));
  }

  private Person getRepresentative(Long representativeId) {
    return personRepository
        .findById(representativeId)
        .orElseThrow(() -> new UserNotFoundException(representativeId));
  }

  private Network getNetwork(Long networkId) {
    return networkRepository
        .findById(networkId)
        .orElseThrow(() -> new EntityNotFoundException(networkId));
  }
}
