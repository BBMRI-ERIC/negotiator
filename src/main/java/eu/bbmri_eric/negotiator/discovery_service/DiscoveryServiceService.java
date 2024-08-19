package eu.bbmri_eric.negotiator.discovery_service;

import eu.bbmri_eric.negotiator.shared.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.shared.exceptions.EntityNotStorableException;
import java.util.List;

public interface DiscoveryServiceService {

  /**
   * Creates a new DiscoveryService in the Negotiator and returns the newly created record
   *
   * @param discoveryServiceCreateDTO a DiscoveryServiceCreateDTO with the data of the
   *     DiscoveryService to create
   * @return a DiscoveryServiceDTO with the data of the newly created DiscoveryService
   * @throws eu.bbmri_eric.negotiator.shared.exceptions.EntityNotStorableException if some error occurs
   *     when creating the DiscoveryService
   */
  DiscoveryServiceDTO create(DiscoveryServiceCreateDTO discoveryServiceCreateDTO)
      throws EntityNotStorableException;

  /**
   * Update the DiscoveryService with id
   *
   * @param id the id of the DiscoveryService to update
   * @param discoveryServiceCreateDTO a DiscoveryServiceCreateDTO with the new data of the
   *     DiscoveryService to updated
   * @return the DiscoveryServiceDTO with the updated data of the DiscoveryService
   */
  DiscoveryServiceDTO update(Long id, DiscoveryServiceCreateDTO discoveryServiceCreateDTO)
      throws EntityNotStorableException;

  /**
   * Returns the list of all DiscoveryServiceDTOs in the Negotiator
   *
   * @return List of all DiscoveryServiceDTO in the negotiator
   */
  List<DiscoveryServiceDTO> findAll();

  /**
   * Retrieve the DiscoveryService with the :id requested
   *
   * @param id the id of the DiscoveryService
   * @return a DiscoveryServiceDTOs with the data of the DiscoveryService with the id requested
   * @throws eu.bbmri_eric.negotiator.shared.exceptions.EntityNotFoundException if the DiscoveryService
   *     identified by :id is not found
   */
  DiscoveryServiceDTO findById(Long id) throws EntityNotFoundException;
}
