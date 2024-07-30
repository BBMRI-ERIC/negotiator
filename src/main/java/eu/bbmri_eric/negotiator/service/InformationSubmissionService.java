package eu.bbmri_eric.negotiator.service;

import eu.bbmri_eric.negotiator.dto.InformationSubmissionDTO;
import eu.bbmri_eric.negotiator.dto.SubmittedInformationDTO;

public interface InformationSubmissionService {
  SubmittedInformationDTO submit(
      InformationSubmissionDTO informationSubmissionDTO,
      Long informationRequirementId,
      String negotiationId);

  SubmittedInformationDTO findById(Long id);
}
