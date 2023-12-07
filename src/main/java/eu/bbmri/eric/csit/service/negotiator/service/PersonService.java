package eu.bbmri.eric.csit.service.negotiator.service;

import eu.bbmri.eric.csit.service.negotiator.database.model.Person;
import eu.bbmri.eric.csit.service.negotiator.database.model.Resource;
import eu.bbmri.eric.csit.service.negotiator.dto.person.UserModel;
import java.util.List;
import java.util.Set;

// TODO: Refactor so only Request and Response models are used.
public interface PersonService {

  /**
   * Retrieves the person identified by :id
   *
   * @param id the id of the person to retrieve
   * @return the
   */
  UserModel findById(Long id);

  /**
   * Retrieves all persons.
   *
   * @return a List of persons.
   */
  List<Person> findAll();

  /**
   * Retrieves a page of people.
   *
   * @param page the page to retrieve.
   * @param size the size of the page.
   * @return a page of people.
   */
  Iterable<UserModel> findAll(int page, int size);

  /**
   * Retrieves page of people from a sorted list.
   *
   * @param page the page to retrieve.
   * @param size the size of the page.
   * @param sort the property to sort by.
   * @return a page of people.
   */
  Iterable<UserModel> findAll(int page, int size, String sort);

  /**
   * Checks if the person with the specified id represents any of the resources in the list.
   *
   * @param personId the id of the person to check;
   * @param resourceExternalIds the list of resource external ids to check;
   * @return true if the person represents any of the resources, false otherwise.
   */
  boolean isRepresentativeOfAnyResource(Long personId, List<String> resourceExternalIds);

  /**
   * Retrieves all resources represented by a person.
   *
   * @param personId the id of the person to retrieve the resources for.
   * @return a Set of resources.
   */
  Set<Resource> getResourcesRepresentedByUserId(Long personId);
}
