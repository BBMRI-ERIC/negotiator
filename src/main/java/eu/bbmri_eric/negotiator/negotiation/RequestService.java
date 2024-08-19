package eu.bbmri_eric.negotiator.negotiation;

import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.common.exceptions.EntityNotStorableException;
import eu.bbmri_eric.negotiator.negotiation.dto.RequestCreateDTO;
import eu.bbmri_eric.negotiator.negotiation.dto.RequestDTO;
import java.util.List;
import java.util.Set;

public interface RequestService {

  /**
   * Creates a new request
   *
   * @param requestBody a RequestCreateDTO with the data of the request to create
   * @return a RequestDTO with the data of the newly created request
   * @throws eu.bbmri_eric.negotiator.common.exceptions.EntityNotStorableException if some error
   *     occurs when creating the Request
   */
  RequestDTO create(RequestCreateDTO requestBody) throws EntityNotStorableException;

  /**
   * Retrieves all the request in the negotiator
   *
   * @return a List of RequestDTO with the data of the requests
   */
  List<RequestDTO> findAll();

  /**
   * Retrieve the request identified by :id
   *
   * @param id the id of the request to retrieve
   * @return a RequestDTO with the data of the request
   * @throws eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException if the request is
   *     not found
   */
  RequestDTO findById(String id) throws EntityNotFoundException;

  /**
   * Retrieves a set of request identified by ids
   *
   * @param ids a Set of ids of the requests to retrieve
   * @return a Set of RequestDTO with the date of the requests
   */
  Set<RequestDTO> findAllById(Set<String> ids);

  /**
   * Update data of the request identified by :id
   *
   * @param id the id of the request to update
   * @param requestBody a RequestCreateDTO with the data of the request
   * @return a RequestDTO with the updated data of the request
   * @throws EntityNotFoundException if the request is not found
   */
  RequestDTO update(String id, RequestCreateDTO requestBody) throws EntityNotFoundException;
}
