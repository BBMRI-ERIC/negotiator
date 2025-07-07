package eu.bbmri_eric.negotiator.user;

import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.common.exceptions.UnsupportedFilterException;
import eu.bbmri_eric.negotiator.common.exceptions.WrongSortingPropertyException;
import eu.bbmri_eric.negotiator.governance.resource.dto.ResourceResponseModel;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Pageable;

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

  List<UserResponseModel> findAllByOrganizationId(Long id);

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

  /**
   * Retrieves all managers of a network.
   *
   * @param networkId the ID of the network to retrieve the managers for.
   * @return a List of UserResponseModel objects representing the managers of the network.
   */
  Iterable<UserResponseModel> findAllForNetwork(Pageable pageable, Long networkId);

  /**
   * Assigns a person as a manager for a network.
   *
   * @param managerId the ID of the person to assign as a manager
   * @param networkId the ID of the network to assign the person as a manager for
   */
  void assignAsManagerForNetwork(Long managerId, Long networkId);

  /**
   * Removes the specified representative's association with a resource.
   *
   * @param managerId the ID of the representative
   * @param networkId the ID of the resource
   */
  void removeAsManagerForNetwork(Long managerId, Long networkId);
}
