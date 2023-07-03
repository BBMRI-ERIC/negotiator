package eu.bbmri.eric.csit.service.negotiator.service;

import eu.bbmri.eric.csit.service.negotiator.dto.negotiation.NegotiationCreateDTO;
import eu.bbmri.eric.csit.service.negotiator.dto.negotiation.NegotiationDTO;
import eu.bbmri.eric.csit.service.negotiator.exceptions.EntityNotFoundException;
import eu.bbmri.eric.csit.service.negotiator.exceptions.EntityNotStorableException;
import java.util.List;

public interface NegotiationService {

  /**
   * Checks if the negotiation with the specified id exist
   *
   * @param negotiationId the id of the Negotiation to check
   * @return true if the Negotiation exist, false otherwise
   */
  boolean exists(String negotiationId);

  /**
   * Creates a new negotiation
   *
   * @param negotiationBody a NegotiationCreateDTO with the data of the negotiation to create
   * @param creatorId the id of the person that wants to create the negotiation
   * @return a NegotiationDTO with the data of the newly created negotiation
   * @throws EntityNotStorableException if some error occurs when crating the negotiation
   */
  NegotiationDTO create(NegotiationCreateDTO negotiationBody, Long creatorId)
      throws EntityNotStorableException;

  /**
   * Updates the negotiation with the provided id
   *
   * @param negotiationBody a NegotiationCreateDTO with the new data of the negotiation to update
   * @param negotiationId the id of the negotiation to update
   * @return a NegotiationDTO with the updated data of the negotiation
   * @throws EntityNotStorableException if some error occurs when crating the negotiation
   * @throws EntityNotFoundException if the negotiation to update is not found
   */
  NegotiationDTO update(String negotiationId, NegotiationCreateDTO negotiationBody)
      throws EntityNotFoundException;

  /**
   * Adds the request with :requestId to the negotiation with identified by :negotiationId
   *
   * @param negotiationId the id of the negotiation
   * @param requestId the id of the request to add to the negotiation
   * @return a NegotiationID with the data of the negotiation
   */
  NegotiationDTO addRequestToNegotiation(String negotiationId, String requestId);

  /**
   * Returns a list of all negotiation in the negotiator
   *
   * @return a List of NegotiationDTO with the data of all negotiation in the negotiator
   */
  List<NegotiationDTO> findAll();

  /**
   * Retrieves the negotiation identified by :id. If includeDetails is true, also details of the
   * negotiation are returned (i.e., data about resources involved in the negotiation)
   *
   * @param id the id
   * @param includeDetails whether to include details about the resources involved in the
   * negotiation or not
   * @return the NegotiationDTO with the data of the negotiation
   * @throws EntityNotFoundException if the requested negotiation is not found
   */
  NegotiationDTO findById(String id, boolean includeDetails);

  // TODO: change byBiobankId
  List<NegotiationDTO> findByBiobankId(String biobankId);

  /**
   * Retrieves a list of negotiations related to a specific resource
   *
   * @param resourceId an id of the resource
   * @return a list of Negotiations
   */
  List<NegotiationDTO> findByResourceId(String resourceId);

  /**
   * Retrieves a list of negotiations of related to the user with id :userId and the role :userRole
   *
   * @param userId the id of the user that has a role in the negotiation
   * @param userRole the role of the user in the negotiation
   * @return a List of NegotiationDTOs
   */
  List<NegotiationDTO> findByUserIdAndRole(String userId, String userRole);

  /**
   * Retrieves a list of negotiations related to specific resources
   *
   * @param resourceIds a List of Ids of resources
   * @return a list of Negotiations
   */
  List<NegotiationDTO> findByResourceIds(List<String> resourceIds);

  /**
   * Retrives a list of negotiations created by user with id
   *
   * @param personId id of the creator
   * @return a list of negotiations
   */
  List<NegotiationDTO> findByCreatorId(Long personId);
}
