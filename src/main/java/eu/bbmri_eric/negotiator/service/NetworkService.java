package eu.bbmri_eric.negotiator.service;

import eu.bbmri_eric.negotiator.dto.NetworkDTO;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface NetworkService {

  /**
   * Retrieves a network with the specified ID.
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

  /**
   * Deletes a network with the specified ID.
   *
   * @param id The ID of the network to delete.
   */
  void deleteNetworkById(Long id);

  /**
   * Removes a resource from a network.
   *
   * @param networkId
   * @param resourceId
   */
  void removeResourceFromNetwork(Long networkId, Long resourceId);

  /**
   * Removes a manager from a network.
   *
   * @param networkId
   * @param managerId
   */
  void removeManagerFromNetwork(Long networkId, Long managerId);

  /**
   * Updates a network with the specified ID.
   *
   * @param id The ID of the network to update.
   * @param networkDTO The updated network data.
   * @return The updated network.
   */
  NetworkDTO updateNetwork(Long id, NetworkDTO networkDTO);

  /**
   * Creates a new network.
   *
   * @param networkDTO The data of the network to create.
   * @return The newly created network.
   */
  NetworkDTO createNetwork(NetworkDTO networkDTO);

  /**
   * Adds a list of managers to a network.
   *
   * @param networkId
   * @param managerIds
   */
  void addManagersToNetwork(Long networkId, List<Long> managerIds);

  /**
   * Adds a list of resources to a network.
   *
   * @param networkId
   * @param resourceIds
   */
  void addResourcesToNetwork(Long networkId, List<Long> resourceIds);
}
