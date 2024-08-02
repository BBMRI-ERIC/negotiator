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

  Iterable<ResourceResponseModel> findAllForNetwork(Pageable pageable, Long networkId);

  List<ResourceWithStatusDTO> findAllInNegotiation(String negotiationId);
}
