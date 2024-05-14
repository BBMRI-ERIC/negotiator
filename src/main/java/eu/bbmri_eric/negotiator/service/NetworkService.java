package eu.bbmri_eric.negotiator.service;

import eu.bbmri_eric.negotiator.dto.NetworkDTO;
import org.springframework.data.domain.Pageable;

public interface NetworkService {

  /**
   * Retrieves a NetworkDTO with the specified ID.
   *
   * @param id The ID of the network to retrieve.
   * @return The NetworkDTO with the specified ID.
   */
  NetworkDTO findNetworkById(Long id);

  /**
   * Retrieves all networks.
   *
   * @return All networks.
   */
  Iterable<NetworkDTO> findAllNetworks(Pageable pageable);
}
