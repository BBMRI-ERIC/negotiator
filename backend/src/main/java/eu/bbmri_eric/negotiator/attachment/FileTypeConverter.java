package eu.bbmri_eric.negotiator.attachment;

import java.io.IOException;

public interface FileTypeConverter {
  byte[] convertToPdf(byte[] docBytes) throws IOException;

  /**
   * Indicates whether this converter supports the given content type.
   *
   * @param contentType the content type to check
   * @return true if the converter supports the content type, false otherwise
   */
  boolean supports(String contentType);
}
