package eu.bbmri_eric.negotiator.attachment;

import java.util.List;

public interface AttachmentConversionService {

  /**
   * Retrieves the attachments of the negoatiation with the specified id and converts them to PDF
   * format.
   *
   * @param negotiationId the id of the negotiation with the attachments
   * @return a list of byte arrays, each representing a PDF file
   * @throws IllegalArgumentException if attachmentIds is null or empty
   */
  List<byte[]> listByNegotiationIdToPdf(String negotiationId);

  /**
   * Retrieves the specified attachments and converts them to PDF format.
   *
   * @param attachmentIds the list of attachment IDs to retrieve and convert
   * @return a list of byte arrays, each representing a PDF file
   * @throws IllegalArgumentException if attachmentIds is null or empty
   */
  List<byte[]> listToPdf(List<String> attachmentIds);
}
