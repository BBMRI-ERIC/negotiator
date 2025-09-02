package eu.bbmri_eric.negotiator.negotiation.pdf;

public interface NegotiationPdfService {

  /**
   * Generates a PDF document for the given negotiation.
   *
   * @param negotiationId the id of the negotiation
   * @param templateName the name of the template to use for rendering
   * @param includeAttachments whether to include attachments to generated PDF or not
   * @return a byte array representing the PDF document
   */
  byte[] generatePdf(String negotiationId, String templateName, boolean includeAttachments);
}
