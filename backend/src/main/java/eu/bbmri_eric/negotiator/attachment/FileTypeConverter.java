package eu.bbmri_eric.negotiator.attachment;

import java.io.IOException;

public interface FileTypeConverter {
  byte[] convertToPdf(byte[] docBytes) throws IOException;
}
