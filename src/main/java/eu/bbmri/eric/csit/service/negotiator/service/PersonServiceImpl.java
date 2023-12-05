package eu.bbmri.eric.csit.service.negotiator.service;

import eu.bbmri.eric.csit.service.negotiator.database.model.Person;
import eu.bbmri.eric.csit.service.negotiator.database.model.Resource;
import eu.bbmri.eric.csit.service.negotiator.database.repository.PersonRepository;
import eu.bbmri.eric.csit.service.negotiator.dto.person.UserModel;
import eu.bbmri.eric.csit.service.negotiator.exceptions.EntityNotFoundException;
import java.util.List;
import java.util.Set;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.stereotype.Service;

@Service(value = "DefaultPersonService")
public class PersonServiceImpl implements PersonService {

  public PersonServiceImpl(PersonRepository personRepository, ModelMapper modelMapper) {
    this.personRepository = personRepository;
    this.modelMapper = modelMapper;
  }

  @Autowired private PersonRepository personRepository;
  @Autowired private ModelMapper modelMapper;

  public UserModel findById(Long id) {
    return modelMapper.map(
        personRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Person with id " + id + " not found")),
        UserModel.class);
  }

  public List<Person> findAll() {
    return personRepository.findAll();
  }

  @Override
  public Iterable<UserModel> findAll(int page, int size) {
    if (page < 0) throw new IllegalArgumentException("Page must be greater than 0.");
    if (size < 1) throw new IllegalArgumentException("Size must be greater than 0.");
    Page<UserModel> result =
        personRepository
            .findAll(PageRequest.of(page, size))
            .map(person -> modelMapper.map(person, UserModel.class));
    if (page > result.getTotalPages())
      throw new IllegalArgumentException(
          "For the given size the page must be less than/equal to " + result.getTotalPages() + ".");
    return result;
  }

  @Override
  public Iterable<UserModel> findAll(int page, int size, String sortProperty) {
    try {
      return personRepository
          .findAll(PageRequest.of(page, size, Sort.by(sortProperty)))
          .map(person -> modelMapper.map(person, UserModel.class));
    } catch (PropertyReferenceException e) {
      throw new IllegalArgumentException(sortProperty + "is an invalid sorting parameter.");
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
  public Set<Resource> getResourcesRepresentedByUserId(Long personId) {
    return personRepository
        .findDetailedById(personId)
        .orElseThrow(() -> new EntityNotFoundException("Person with id " + personId + " not found"))
        .getResources();
  }
}
