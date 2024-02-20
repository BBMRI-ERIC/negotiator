package eu.bbmri_eric.negotiator.service;

import eu.bbmri_eric.negotiator.dto.access_criteria.AccessCriteriaSetDTO;

public interface AccessFormService {
  /**
   * Get an access form combining the access criteria of the resources in the request.
   *
   * @param requestId The request id.
   * @return The access form.
   */
  AccessCriteriaSetDTO getAccessFormForRequest(String requestId);
}
