package eu.bbmri_eric.negotiator.service;

import eu.bbmri_eric.negotiator.database.model.Network;
import eu.bbmri_eric.negotiator.database.model.Person;
import eu.bbmri_eric.negotiator.database.model.Resource;
import eu.bbmri_eric.negotiator.database.repository.NetworkRepository;
import eu.bbmri_eric.negotiator.database.repository.PersonRepository;
import eu.bbmri_eric.negotiator.database.repository.PersonSpecifications;
import eu.bbmri_eric.negotiator.database.repository.ResourceRepository;
import eu.bbmri_eric.negotiator.dto.person.ResourceResponseModel;
import eu.bbmri_eric.negotiator.dto.person.UserResponseModel;
import eu.bbmri_eric.negotiator.events.FirstRepresentativeEvent;
import eu.bbmri_eric.negotiator.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.exceptions.UnsupportedFilterException;
import eu.bbmri_eric.negotiator.exceptions.UserNotFoundException;
import eu.bbmri_eric.negotiator.exceptions.WrongSortingPropertyException;
import jakarta.transaction.Transactional;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.apachecommons.CommonsLog;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.stereotype.Service;

@Service
@Transactional
@CommonsLog
public class PersonServiceImpl implements PersonService {

  @Autowired NetworkRepository networkRepository;

  public PersonServiceImpl(
      PersonRepository personRepository,
      ResourceRepository resourceRepository,
      ModelMapper modelMapper,
      ApplicationEventPublisher eventPublisher) {
    this.personRepository = personRepository;
    this.resourceRepository = resourceRepository;
    this.modelMapper = modelMapper;
    this.eventPublisher = eventPublisher;
  }

  private final PersonRepository personRepository;

  private final ResourceRepository resourceRepository;
  private final ModelMapper modelMapper;
  private final ApplicationEventPublisher eventPublisher;

  public UserResponseModel findById(Long id) {
    return modelMapper.map(getRepresentative(id), UserResponseModel.class);
  }

  @Override
  public Iterable<UserResponseModel> findAllByFilter(
      String property, String matchedValue, int page, int size) {
    if (page < 0) throw new IllegalArgumentException("Page must be greater than 0.");
    if (size < 1) throw new IllegalArgumentException("Size must be greater than 0.");
    if (Arrays.stream(UserResponseModel.class.getDeclaredFields())
        .anyMatch(field -> field.getName().equals(property))) {
      Page<UserResponseModel> result =
          personRepository
              .findAll(
                  PersonSpecifications.propertyEquals(property, matchedValue),
                  PageRequest.of(page, size))
              .map(person -> modelMapper.map(person, UserResponseModel.class));
      if (page > result.getTotalPages())
        throw new IllegalArgumentException(
            "For the given size the page must be less than/equal to "
                + result.getTotalPages()
                + ".");
      return result;
    } else
      throw new UnsupportedFilterException(
          property,
          Arrays.stream(UserResponseModel.class.getFields())
              .map(Field::getName)
              .toArray(String[]::new));
  }

  @Override
  public Iterable<UserResponseModel> findAll(int page, int size) {
    if (page < 0) throw new IllegalArgumentException("Page must be greater than 0.");
    if (size < 1) throw new IllegalArgumentException("Size must be greater than 0.");
    Page<UserResponseModel> result =
        personRepository
            .findAll(PageRequest.of(page, size))
            .map(person -> modelMapper.map(person, UserResponseModel.class));
    if (page > result.getTotalPages())
      throw new IllegalArgumentException(
          "For the given size the page must be less than/equal to " + result.getTotalPages() + ".");
    return result;
  }

  @Override
  public Iterable<UserResponseModel> findAll(int page, int size, String sortProperty) {
    try {
      return personRepository
          .findAll(PageRequest.of(page, size, Sort.by(sortProperty)))
          .map(person -> modelMapper.map(person, UserResponseModel.class));
    } catch (PropertyReferenceException e) {
      throw new WrongSortingPropertyException(sortProperty, "name, id, email, organization");
    }
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
}
