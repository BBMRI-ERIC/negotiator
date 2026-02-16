package eu.bbmri_eric.negotiator.info_submission;

import java.util.List;

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
  InformationSubmissionSummaryDTO createSummary(Long requirementId, String negotiationId);

  /**
   * Generates a PDF summary for a specific requirement within a negotiation.
   *
   * @param requirementId the ID of the requirement
   * @param negotiationId the ID of the negotiation
   * @return a byte array containing the PDF data
   */
  byte[] createPdfSummary(Long requirementId, String negotiationId);

  /**
   * Generates all PDF summaries for a given negotiation.
   *
   * @param negotiationId the ID of the negotiation
   * @return a list of byte arrays, each containing a PDF summary
   */
  List<byte[]> createAllPdfSummaries(String negotiationId);
}
