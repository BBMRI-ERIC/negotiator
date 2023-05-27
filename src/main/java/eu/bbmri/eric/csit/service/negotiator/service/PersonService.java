package eu.bbmri.eric.csit.service.negotiator.service;

import eu.bbmri.eric.csit.service.negotiator.dto.perun.PerunUserDTO;
import eu.bbmri.eric.csit.service.negotiator.database.model.Person;

import java.util.List;

public interface PersonService {

  /**
   * Retrieves the person identified by :id
   * @param id the id of the person to retrieve
   * @return the
   */
  Person findById(Long id);

  List<Person> findAll();

  List<PerunUserDTO> createOrUpdate(List<PerunUserDTO> request);

  PerunUserDTO save();

}
