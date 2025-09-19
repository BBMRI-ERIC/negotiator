package eu.bbmri_eric.negotiator.attachment;

import eu.bbmri_eric.negotiator.common.exceptions.PdfGenerationException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import lombok.extern.apachecommons.CommonsLog;
import org.docx4j.Docx4J;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;

@CommonsLog
class DocxConverter implements FileTypeConverter {
  @Override
  public byte[] convertToPdf(byte[] docBytes) throws IOException, PdfGenerationException {
    if (docBytes == null || docBytes.length == 0) {
      throw new IllegalArgumentException("Input DOCX bytes are null or empty");
    }

    log.debug("Converting DOCX to PDF, input size: " + docBytes.length);

    try (ByteArrayInputStream docxInputStream = new ByteArrayInputStream(docBytes);
        ByteArrayOutputStream pdfOutputStream = new ByteArrayOutputStream()) {
      WordprocessingMLPackage wordMLPackage = Docx4J.load(docxInputStream);
      if (wordMLPackage == null) {
        throw new IllegalStateException("Failed to load DOCX package");
      }
      Docx4J.toPDF(wordMLPackage, pdfOutputStream);
      byte[] result = pdfOutputStream.toByteArray();
      log.debug("Successfully converted DOCX to PDF, output size: " + result.length);
      return result;
    } catch (Docx4JException e) {
      throw new PdfGenerationException();
    }
  }
}
