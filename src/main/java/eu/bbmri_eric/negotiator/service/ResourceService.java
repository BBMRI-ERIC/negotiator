package eu.bbmri_eric.negotiator.service;

import eu.bbmri_eric.negotiator.dto.person.ResourceResponseModel;
import eu.bbmri_eric.negotiator.dto.resource.ResourceWithStatusDTO;
import java.util.List;
import org.springframework.data.domain.Pageable;

/** The ResourceService interface defines the contract for accessing and manipulating resources. */
public interface ResourceService {
  /**
   * Retrieves a ResourceResponseModel with the specified ID.
   *
   * @param id The ID of the resource to retrieve.
   * @return The ResourceResponseModel with the specified ID.
   */
  ResourceResponseModel findById(Long id);

  /**
   * Retrieves all resources.
   *
   * @return All resources.
   */
  Iterable<ResourceResponseModel> findAll(Pageable pageable);

  /**
   * Retrieve all resources linked to a Network.
   *
   * @param pageable Pageable request
   * @param networkId id of the network
   * @return an iterable collection of Resources
   */
  Iterable<ResourceResponseModel> findAllForNetwork(Pageable pageable, Long networkId);

  /**
   * Find all Resources involved in a specific Negotiation
   *
   * @param negotiationId the id of the Negotiation
   * @return a list of resources
   */
  List<ResourceWithStatusDTO> findAllInNegotiation(String negotiationId);
}
