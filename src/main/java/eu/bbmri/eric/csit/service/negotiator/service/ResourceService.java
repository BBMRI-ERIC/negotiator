package eu.bbmri.eric.csit.service.negotiator.service;

import eu.bbmri.eric.csit.service.negotiator.dto.person.ResourceResponseModel;

public interface ResourceService {
  ResourceResponseModel findById(Long id);
}
