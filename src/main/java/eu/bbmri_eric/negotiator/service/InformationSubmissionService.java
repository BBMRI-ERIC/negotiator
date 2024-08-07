package eu.bbmri_eric.negotiator.service;

import eu.bbmri_eric.negotiator.dto.InformationSubmissionDTO;
import eu.bbmri_eric.negotiator.dto.SubmittedInformationDTO;
import java.io.IOException;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface InformationSubmissionService {
  /**
   * Submit required Information for the given resource in a Negotiation.
   *
   * @param informationSubmissionDTO a DTO containing necessary information
   * @param informationRequirementId id of the requirement
   * @param negotiationId id of the negotiation
   * @return submitted information
   */
  SubmittedInformationDTO submit(
      InformationSubmissionDTO informationSubmissionDTO,
      Long informationRequirementId,
      String negotiationId);

  /**
   * Find an information submission by ID.
   *
   * @param id the ID of the sought submission
   * @return the submitted information
   */
  SubmittedInformationDTO findById(Long id);

  /**
   * Find all submissions for a given Negotiation.
   *
   * @param negotiationId the ID of the Negotiation
   * @return a list of all linked submissions
   */
  List<SubmittedInformationDTO> findAllForNegotiation(String negotiationId);

  /**
   * Generate a summary of all submissions for a given requirement.
   *
   * @return a summary file
   */
  MultipartFile createSummary(Long requirementId, String negotiationId) throws IOException;
}
