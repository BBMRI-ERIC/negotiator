package eu.bbmri.eric.csit.service.negotiator.service;

import eu.bbmri.eric.csit.service.negotiator.dto.person.ResourceResponseModel;

/** The ResourceService interface defines the contract for accessing and manipulating resources. */
public interface ResourceService {
  /**
   * Retrieves a ResourceResponseModel with the specified ID.
   *
   * @param id The ID of the resource to retrieve.
   * @return The ResourceResponseModel with the specified ID.
   */
  ResourceResponseModel findById(Long id);
}
