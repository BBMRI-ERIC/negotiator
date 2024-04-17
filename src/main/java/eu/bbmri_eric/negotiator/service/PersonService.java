package eu.bbmri_eric.negotiator.service;

import eu.bbmri_eric.negotiator.dto.person.ResourceResponseModel;
import eu.bbmri_eric.negotiator.dto.person.UserResponseModel;
import eu.bbmri_eric.negotiator.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.exceptions.UnsupportedFilterException;
import eu.bbmri_eric.negotiator.exceptions.WrongSortingPropertyException;
import java.util.List;
import java.util.Set;

public interface PersonService {

  /**
   * Retrieves the person identified by :id
   *
   * @param id the id of the person to retrieve
   * @return the
   */
  UserResponseModel findById(Long id) throws EntityNotFoundException;

  /**
   * Retrieves all UserResponseModels that match the specified filter criteria.
   *
   * @param property the property to filter on
   * @param matchedValue the value that must be matched
   * @return an Iterable of UserResponseModel objects that match the filter criteria
   */
  Iterable<UserResponseModel> findAllByFilter(
      String property, String matchedValue, int page, int size) throws UnsupportedFilterException;

  /**
   * Retrieves a page of people.
   *
   * @param page the page to retrieve.
   * @param size the size of the page.
   * @return a page of people.
   */
  Iterable<UserResponseModel> findAll(int page, int size);

  /**
   * Retrieves page of people from a sorted list.
   *
   * @param page the page to retrieve.
   * @param size the size of the page.
   * @param sort the property to sort by.
   * @return a page of people.
   */
  Iterable<UserResponseModel> findAll(int page, int size, String sort)
      throws WrongSortingPropertyException;

  /**
   * Checks if the person with the specified id represents any of the resources in the list.
   *
   * @param personId the id of the person to check;
   * @param resourceExternalIds the list of resource external ids to check;
   * @return true if the person represents any of the resources, false otherwise.
   */
  boolean isRepresentativeOfAnyResource(Long personId, List<String> resourceExternalIds);

  /**
   * Checks if the person with the specified id represents any of the resources of an organization
   *
   * @param personId the id of the person to check;
   * @param organizationId the list of resource external ids to check;
   * @return true if the person represents any of the resources, false otherwise.
   */
  boolean isRepresentativeOfAnyResourceOfOrganization(Long personId, Long organizationId);

  /**
   * Checks if the person with the specified id represents any of the resources of the organization
   * identified by the externalId
   *
   * @param personId the id of the person to check;
   * @param organizationExternalId the list of resource external ids to check;
   * @return true if the person represents any of the resources, false otherwise.
   */
  boolean isRepresentativeOfAnyResourceOfOrganization(Long personId, String organizationExternalId);

  /**
   * Checks whether a person is representative of at least one resource that is involved in the
   * Negotiation
   *
   * @param personId the id of the person to check
   * @param negotiationId the id of the negotiation with the resources to check
   * @return true if the person represents any of the resources of the negotiation, false otherwise
   */
  boolean isRepresentativeOfAnyResourceOfNegotiation(Long personId, String negotiationId);

  /**
   * Retrieves all resources represented by a person.
   *
   * @param personId the id of the person to retrieve the resources for.
   * @return a Set of resources.
   */
  Set<ResourceResponseModel> getResourcesRepresentedByUserId(Long personId);

  /**
   * Assigns a resource to a representative.
   *
   * @param representativeId the ID of the representative
   * @param resourceId the ID of the resource to be assigned
   */
  void assignAsRepresentativeForResource(Long representativeId, Long resourceId);

  /**
   * Removes the specified representative's association with a resource.
   *
   * @param representativeId the ID of the representative
   * @param resourceId the ID of the resource
   */
  void removeAsRepresentativeForResource(Long representativeId, Long resourceId);
}
