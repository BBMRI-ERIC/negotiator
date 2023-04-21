package eu.bbmri.eric.csit.service.negotiator.service;

import eu.bbmri.eric.csit.service.negotiator.api.dto.access_criteria.AccessCriteriaSetDTO;

public interface AccessCriteriaSetService {

  AccessCriteriaSetDTO findByResourceEntityId(String resourceEntityId);

}
