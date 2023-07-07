package eu.bbmri.eric.csit.service.negotiator.service;

import eu.bbmri.eric.csit.service.negotiator.database.model.Person;
import eu.bbmri.eric.csit.service.negotiator.database.repository.PersonRepository;
import eu.bbmri.eric.csit.service.negotiator.dto.perun.PerunUserDTO;
import eu.bbmri.eric.csit.service.negotiator.exceptions.EntityNotFoundException;
import java.util.ArrayList;
import java.util.List;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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

  public List<PerunUserDTO> createOrUpdate(List<PerunUserDTO> perunUserDTOS) {
    List<PerunUserDTO> users = new ArrayList<>();
    for (PerunUserDTO user : perunUserDTOS) {
      Person person = getByAuthSubject(String.valueOf(user.getId()));
      try {
        if (person == null) {
          person = modelMapper.map(user, Person.class);
        } else {
          modelMapper.map(user, person);
        }
        users.add(modelMapper.map(personRepository.save(person), PerunUserDTO.class));
      } catch (DataIntegrityViolationException ex) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Some data sent cannot be saved");
      }
    }
    return users;
  }
}
