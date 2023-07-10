package eu.bbmri.eric.csit.service.negotiator.service;

import eu.bbmri.eric.csit.service.negotiator.dto.access_criteria.AccessCriteriaSetDTO;

public interface AccessCriteriaSetService {

  /**
   * Returns the AccessCriteriaSetDTO of the Resource with the requested id
   *
   * @param resourceId the ID of the Resource
   * @return an AccessCriteriaSetDTO containing the AccessCriteria of the Resource
   */
  AccessCriteriaSetDTO findByResourceId(String resourceId);
}
