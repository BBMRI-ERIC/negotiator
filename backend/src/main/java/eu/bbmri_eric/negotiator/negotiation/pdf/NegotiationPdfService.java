package eu.bbmri_eric.negotiator.negotiation.pdf;

import eu.bbmri_eric.negotiator.negotiation.Negotiation;

public interface NegotiationPdfService {

  /**
   * Generates a PDF document for the given negotiation.
   *
   * @param negotiation the negotiation object containing the data to be included in the PDF
   * @param templateName the name of the template to use for rendering
   * @return a byte array representing the PDF document
   * @throws Exception if an error occurs during PDF generation
   */
  byte[] generatePdf(Negotiation negotiation, String templateName) throws Exception;
}
