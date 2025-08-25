package eu.bbmri_eric.negotiator.negotiation.pdf;

public interface NegotiationPdfService {

  /**
   * Generates a PDF document for the given negotiation.
   *
   * @param negotiationId the id of the negotiation
   * @return a byte array representing the PDF document
   */
  byte[] generatePdf(String negotiationId);
}
