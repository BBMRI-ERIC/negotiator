package eu.bbmri_eric.negotiator.negotiation.pdf;

public interface NegotiationPdfService {

  /**
   * Generates a PDF document for the given negotiation.
   *
   * @param negotiationId the id of the negotiation
   * @param templateName the name of the template to use for rendering
   * @return a byte array representing the PDF document
   * @throws Exception if an error occurs during PDF generation
   */
  byte[] generatePdf(String negotiationId, String templateName) throws Exception;
}
