package eu.bbmri.eric.csit.service.negotiator.service;

import eu.bbmri.eric.csit.service.negotiator.database.model.Person;
import eu.bbmri.eric.csit.service.negotiator.database.model.Resource;
import eu.bbmri.eric.csit.service.negotiator.database.repository.PersonRepository;
import eu.bbmri.eric.csit.service.negotiator.database.repository.PersonSpecifications;
import eu.bbmri.eric.csit.service.negotiator.database.repository.ResourceRepository;
import eu.bbmri.eric.csit.service.negotiator.dto.person.ResourceResponseModel;
import eu.bbmri.eric.csit.service.negotiator.dto.person.UserResponseModel;
import eu.bbmri.eric.csit.service.negotiator.exceptions.EntityNotFoundException;
import eu.bbmri.eric.csit.service.negotiator.exceptions.UnsupportedFilterException;
import eu.bbmri.eric.csit.service.negotiator.exceptions.UserNotFoundException;
import eu.bbmri.eric.csit.service.negotiator.exceptions.WrongSortingPropertyException;
import jakarta.transaction.Transactional;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class PersonServiceImpl implements PersonService {

  public PersonServiceImpl(
      PersonRepository personRepository,
      ResourceRepository resourceRepository,
      ModelMapper modelMapper) {
    this.personRepository = personRepository;
    this.resourceRepository = resourceRepository;
    this.modelMapper = modelMapper;
  }

  private final PersonRepository personRepository;

  private final ResourceRepository resourceRepository;
  private final ModelMapper modelMapper;

  public UserResponseModel findById(Long id) {
    return modelMapper.map(
        personRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id)),
        UserResponseModel.class);
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
  public void assignResourceForRepresentation(Long representativeId, Long resourceId) {
    Person representative =
        personRepository
            .findById(representativeId)
            .orElseThrow(() -> new UserNotFoundException(representativeId));
    Resource resource =
        resourceRepository
            .findById(resourceId)
            .orElseThrow(() -> new EntityNotFoundException(resourceId));
    representative.addResource(resource);
    personRepository.save(representative);
  }
}
