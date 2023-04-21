package eu.bbmri.eric.csit.service.negotiator.service;

import eu.bbmri.eric.csit.service.negotiator.api.dto.perun.PerunUserRequest;
import eu.bbmri.eric.csit.service.negotiator.database.model.Person;
import eu.bbmri.eric.csit.service.negotiator.database.repository.PersonRepository;
import java.util.List;
import javax.annotation.PostConstruct;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PersonService {

  @Autowired
  private PersonRepository personRepository;

  @Autowired
  private ModelMapper modelMapper;

  public Person getById(Long id) {
    return personRepository
        .findById(id)
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND, String.format("Person with id %d not found", id)));
  }

  public Person getByAuthSubject(String authSubject) {
    return personRepository.findByAuthSubject(authSubject).orElse(null);
  }

  public List<Person> findAll() {
    return personRepository.findAll();
  }

  public Person createOrUpdate(PerunUserRequest perunUserRequest) {

    Person pers = getByAuthSubject(String.format("%s", perunUserRequest.getId()));
    try {
      if (pers == null) {
        Person personEntity = modelMapper.map(perunUserRequest, Person.class);
        return personRepository.save(personEntity);
      }
      modelMapper.map(perunUserRequest, pers);
      return personRepository.save(pers);
    } catch (DataIntegrityViolationException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Some data sent cannot be saved");
    }
  }

  //  public Person create(PerunUserRequest perunUserRequest) {
  //
  //    Person personEntity = modelMapper.map(perunUserRequest, Person.class);
  //    try {
  //      return personRepository.save(personEntity);
  //    } catch (DataIntegrityViolationException ex) {
  //      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Some data sent cannot be
  // saved");
  //    }
  //  }
  //
  //  public Person update(Integer id, PerunUserRequest perunUserRequest) {
  //    Person personEntity = getByAuthSubject(String.format("%s",id));
  //    modelMapper.map(perunUserRequest, personEntity);
  //    try {
  //      return personRepository.save(personEntity);
  //    } catch (DataIntegrityViolationException ex) {
  //      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Some data sent cannot be
  // saved");
  //    }
  //  }

}
