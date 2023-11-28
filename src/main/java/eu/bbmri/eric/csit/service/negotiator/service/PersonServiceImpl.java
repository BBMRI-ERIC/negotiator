package eu.bbmri.eric.csit.service.negotiator.service;

import eu.bbmri.eric.csit.service.negotiator.database.model.Person;
import eu.bbmri.eric.csit.service.negotiator.database.model.Resource;
import eu.bbmri.eric.csit.service.negotiator.database.repository.PersonRepository;
import eu.bbmri.eric.csit.service.negotiator.exceptions.EntityNotFoundException;
import java.util.List;
import java.util.Set;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service(value = "DefaultPersonService")
public class PersonServiceImpl implements PersonService {

  @Autowired private PersonRepository personRepository;

  @Autowired private ModelMapper modelMapper;

  public Person findById(Long id) {
    return personRepository.findById(id).orElseThrow(() -> new EntityNotFoundException(id));
  }

  private Person getByAuthSubject(String authSubject) {
    return personRepository.findByAuthSubject(authSubject).orElse(null);
  }

  public List<Person> findAll() {
    return personRepository.findAll();
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
        .findById(personId)
        .orElseThrow(() -> new EntityNotFoundException("Person with id " + personId + " not found"))
        .getResources();
  }
}
