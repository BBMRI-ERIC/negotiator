package eu.bbmri_eric.negotiator.attachment;

import eu.bbmri_eric.negotiator.common.exceptions.PdfGenerationException;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;

@CommonsLog
class PDFConverter implements FileTypeConverter {
  private static final String CONTENT_TYPE_PDF = "application/pdf";

  @Override
  public byte[] convertToPdf(byte[] docBytes) throws PdfGenerationException {
    log.debug("Attachment is already PDF, returning as-is");
    return docBytes;
  }

  @Override
  public boolean supports(String contentType) {
    return StringUtils.equals(contentType, CONTENT_TYPE_PDF);
  }
}
