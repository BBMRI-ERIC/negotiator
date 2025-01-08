package eu.bbmri_eric.negotiator.form.service;

import eu.bbmri_eric.negotiator.form.dto.AccessFormDTO;

public interface AccessCriteriaSetService {

  /**
   * Returns the AccessFormDTO of the Resource with the requested id
   *
   * @param resourceId the ID of the Resource
   * @return an AccessFormDTO containing the AccessFormElement of the Resource
   */
  AccessFormDTO findByResourceId(String resourceId);
}
