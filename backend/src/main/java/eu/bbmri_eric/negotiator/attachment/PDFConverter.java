package eu.bbmri_eric.negotiator.attachment;

import eu.bbmri_eric.negotiator.common.exceptions.PdfGenerationException;

class PDFConverter implements FileTypeConverter {

  @Override
  public byte[] convertToPdf(byte[] docBytes) throws PdfGenerationException {
    return docBytes;
  }
}
