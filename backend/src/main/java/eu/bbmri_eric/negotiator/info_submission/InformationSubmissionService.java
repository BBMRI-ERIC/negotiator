package eu.bbmri_eric.negotiator.info_submission;

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
   * Update submission of required Information for the given resource in a Negotiation.
   *
   * @param informationSubmissionDTO a DTO containing necessary information
   * @param submissionId id of the negotiation
   * @return updated submitted information
   */
  SubmittedInformationDTO updateSubmission(
      InformationSubmissionDTO informationSubmissionDTO, Long submissionId);

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
  MultipartFile createSummary(Long requirementId, String negotiationId);

  /**
   * Generate a PDF summary of all submissions for a given requirement.
   *
   * @param requirementId the ID of the requirement
   * @param negotiationId the ID of the negotiation
   * @return a PDF file containing the summary
   */
  byte[] createPdfSummary(Long requirementId, String negotiationId);

  /**
   * Generate PDF summaries for all information requirements in a negotiation.
   *
   * @param negotiationId the ID of the negotiation
   * @return a list of PDF byte arrays, one for each requirement
   */
  List<byte[]> createAllPdfSummaries(String negotiationId);
}
