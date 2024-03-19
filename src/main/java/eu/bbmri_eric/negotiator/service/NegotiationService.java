package eu.bbmri_eric.negotiator.service;

import eu.bbmri_eric.negotiator.configuration.state_machine.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.database.model.Negotiation;
import eu.bbmri_eric.negotiator.dto.negotiation.NegotiationCreateDTO;
import eu.bbmri_eric.negotiator.dto.negotiation.NegotiationDTO;
import eu.bbmri_eric.negotiator.dto.negotiation.NegotiationFilters;
import eu.bbmri_eric.negotiator.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.exceptions.EntityNotStorableException;
import java.util.List;
import org.springframework.data.domain.Pageable;

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
   * @throws eu.bbmri_eric.negotiator.exceptions.EntityNotStorableException if some error occurs
   *     when crating the negotiation
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
   * @throws eu.bbmri_eric.negotiator.exceptions.EntityNotFoundException if the negotiation to
   *     update is not found
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
  Iterable<NegotiationDTO> findAll(Pageable pageable);

  /**
   * Returns a paged list of all negotiations in the negotiator with the specified current state.
   *
   * @param pageable the page request
   * @param state the current state of the negotiations
   * @return a paged list of NegotiationDTOs
   */
  Iterable<NegotiationDTO> findAllByCurrentStatus(Pageable pageable, NegotiationState state);

  Iterable<NegotiationDTO> findByFilters(
      Pageable pageable, NegotiationFilters filters, Long userId);

  /**
   * Retrieves the negotiation identified by :id. If includeDetails is true, also details of the
   * negotiation are returned (i.e., data about resources involved in the negotiation)
   *
   * @param id the id
   * @param includeDetails whether to include details about the resources involved in the
   *     negotiation or not
   * @return the NegotiationDTO with the data of the negotiation
   * @throws EntityNotFoundException if the requested negotiation is not found
   */
  NegotiationDTO findById(String id, boolean includeDetails);

  /**
   * Sets the enabledPosts attrubute to true for the input Negotiation
   *
   * @param negotiationId the ID of the Negotiation
   */
  void enablePosts(String negotiationId);

  /**
   * Sets the enabledPosts attrubute to false for the input Negotiation
   *
   * @param negotiationId the ID of the Negotiation
   */
  void disablePosts(String negotiationId);

  /**
   * Retrieves all Negotiations with specific current state.
   *
   * @param negotiationState current state of interest.
   * @return A list of NegotiationDTOs with specific state.
   */
  List<NegotiationDTO> findAllWithCurrentState(NegotiationState negotiationState);

  boolean isAuthorizedForNegotiation(Negotiation negotiation);
}
