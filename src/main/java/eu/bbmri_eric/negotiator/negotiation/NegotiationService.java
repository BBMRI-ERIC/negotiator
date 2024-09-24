package eu.bbmri_eric.negotiator.negotiation;

import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.common.exceptions.EntityNotStorableException;
import eu.bbmri_eric.negotiator.negotiation.dto.NegotiationCreateDTO;
import eu.bbmri_eric.negotiator.negotiation.dto.NegotiationDTO;
import eu.bbmri_eric.negotiator.negotiation.dto.NegotiationFilters;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationState;
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
   * @throws eu.bbmri_eric.negotiator.common.exceptions.EntityNotStorableException if some error
   *     occurs when crating the negotiation
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
   * @throws eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException if the negotiation
   *     to update is not found
   */
  NegotiationDTO update(String negotiationId, NegotiationCreateDTO negotiationBody)
      throws EntityNotFoundException;

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

  /**
   * Retrieves all the Negotiations, using pagination, filtered by provided filters and
   *
   * @param pageable A Pageable object desribing the required pagination
   * @param filters A NegotiationFilters with filters to apply
   * @return A paged list of NegotiationDTOs filtered using specific filters
   */
  Iterable<NegotiationDTO> findAllByFilters(Pageable pageable, NegotiationFilters filters);

  /**
   * Retrieves the negotiations, using pagination, filtered by provided filters and related (i.e.,
   * he or she is the AUTHOR or the REPRESENTATIVE) to a user
   *
   * @param pageable A Pageable object desribing the required pagination
   * @param filters A NegotiationFilters with filters to apply
   * @param userId The userId of the user that is involved (AUTHOR or REPR) in the Negotiations
   * @return A paged list of NegotiationDTOs where the user is involved, filtered using specific
   *     filters
   */
  Iterable<NegotiationDTO> findByFiltersForUser(
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
   * Enables or disables private posts for negotiation with id negotiationId
   *
   * @param negotiationId the ID of the Negotiation
   * @param enabled whether to enable or disable the private posts
   */
  void setPrivatePostsEnabled(String negotiationId, boolean enabled);

  /**
   * Enables or disables public posts for negotiation with id negotiationId
   *
   * @param negotiationId the ID of the Negotiation
   * @param enabled whether to enable or disable the public posts
   */
  void setPublicPostsEnabled(String negotiationId, boolean enabled);

  /**
   * Retrieves all Negotiations with specific current state.
   *
   * @param negotiationState current state of interest.
   * @return A list of NegotiationDTOs with specific state.
   */
  List<NegotiationDTO> findAllWithCurrentState(NegotiationState negotiationState);

  /**
   * Checks whether the currently authenticated user is authorized for negotiation
   *
   * @param negotiationId the id of the negotiaton to check
   * @return true if the authenticated user is authorized, false otherwise
   */
  boolean isAuthorizedForNegotiation(String negotiationId);

  /**
   * Checks whether the currently authenticated user is creator of negotiation
   *
   * @param negotiationId the id of the negotiaton to check
   * @return true if the authenticated user is the creator of the negotiation, false otherwise
   */
  boolean isNegotiationCreator(String negotiationId);

  /**
   * Checks whether a an Organization is part of the negotiation, i.e., there is at least one
   * resource part of the Organization that is part of the Negotiation
   *
   * @param negotiationId the negotiationId
   * @param organizationExternalId the organizationExternalId
   * @return true whether at least one resource of the organization is part of the negotiation,
   *     false otherwise
   */
  boolean isOrganizationPartOfNegotiation(String negotiationId, String organizationExternalId);

  /**
   * Retrieves all negotiations related to a network
   *
   * @param pageable the page request
   * @param networkId the id of the network
   * @return a paged list of NegotiationDTOs
   */
  Iterable<NegotiationDTO> findAllForNetwork(Pageable pageable, Long networkId);
}
