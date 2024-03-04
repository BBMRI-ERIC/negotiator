package eu.bbmri_eric.negotiator.service;

import eu.bbmri_eric.negotiator.dto.access_form.AccessFormDTO;

public interface AccessFormService {
  /**
   * Get an access form combining the access criteria of the resources in the request.
   *
   * @param requestId The request id.
   * @return The access form.
   */
  AccessFormDTO getAccessFormForRequest(String requestId);

  /**
   * Get an access form by id.
   *
   * @param id The access form id.
   * @return The access form.
   */
  AccessFormDTO getAccessForm(Long id);
}
