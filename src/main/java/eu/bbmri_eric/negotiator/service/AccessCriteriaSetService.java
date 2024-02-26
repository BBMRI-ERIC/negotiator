package eu.bbmri_eric.negotiator.service;

import eu.bbmri_eric.negotiator.dto.access_criteria.AccessCriteriaSetDTO;

public interface AccessCriteriaSetService {

  /**
   * Returns the AccessCriteriaSetDTO of the Resource with the requested id
   *
   * @param resourceId the ID of the Resource
   * @return an AccessCriteriaSetDTO containing the AccessFormElement of the Resource
   */
  AccessCriteriaSetDTO findByResourceId(String resourceId);
}
