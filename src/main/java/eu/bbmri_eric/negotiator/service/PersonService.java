package eu.bbmri_eric.negotiator.service;

import eu.bbmri_eric.negotiator.database.model.Person;
import java.util.List;

public interface PersonService {

  /**
   * Retrieves the person identified by :id
   *
   * @param id the id of the person to retrieve
   * @return the
   */
  Person findById(Long id);

  List<Person> findAll();
}
