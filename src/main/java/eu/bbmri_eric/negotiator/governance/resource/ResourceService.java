package eu.bbmri_eric.negotiator.governance.resource;

import eu.bbmri_eric.negotiator.common.FilterDTO;
import eu.bbmri_eric.negotiator.governance.resource.dto.ResourceCreateDTO;
import eu.bbmri_eric.negotiator.governance.resource.dto.ResourceDTO;
import eu.bbmri_eric.negotiator.governance.resource.dto.ResourceResponseModel;
import eu.bbmri_eric.negotiator.governance.resource.dto.ResourceWithStatusDTO;
import eu.bbmri_eric.negotiator.negotiation.dto.UpdateResourcesDTO;
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
  Iterable<ResourceResponseModel> findAll(FilterDTO filters);

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

  /**
   * Edit resources to a Negotiation. Any Resources in the list that are not already a part of the
   * Negotiation will be added. Warning: If you supply a state, all resources will be updated to
   * that state.
   *
   * @param negotiationId a specific Negotiation
   * @param updateResourcesDTO a list of resource IDs to be added or updated
   * @return an updated list of resources
   */
  List<ResourceWithStatusDTO> updateResourcesInANegotiation(
      String negotiationId, UpdateResourcesDTO updateResourcesDTO);

  /**
   * Add a batch of resources. This method is uses from a specific synchronization servvice that
   * checks if one or more resources are already present, and eventually add them.
   *
   * @param List<ResourceCreateDTO> a list of all the resources to add
   * @return the output DTO list of all the added resources
   */
  List<ResourceDTO> addResources(List<ResourceCreateDTO> resources);
}
