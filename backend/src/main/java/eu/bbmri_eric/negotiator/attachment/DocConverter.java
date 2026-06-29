package eu.bbmri_eric.negotiator.attachment;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.usermodel.Range;

@CommonsLog
class DocConverter implements FileTypeConverter {
  @Override
  public byte[] convertToPdf(byte[] docBytes) throws IOException {
    if (docBytes == null || docBytes.length == 0) {
      throw new IllegalArgumentException("Input DOC bytes are null or empty");
    }

    log.debug("Converting DOC to PDF, input size: " + docBytes.length);
    Document pdfDoc = null;
    PdfDocument pdfDocument = null;

    try (ByteArrayInputStream docInputStream = new ByteArrayInputStream(docBytes);
        HWPFDocument doc = new HWPFDocument(docInputStream);
        ByteArrayOutputStream pdfOutputStream = new ByteArrayOutputStream()) {

      Range range = doc.getRange();
      int paragraphCount = range.numParagraphs();
      log.debug("Processing paragraphs from DOC: " + paragraphCount);

      pdfDocument = new PdfDocument(new PdfWriter(pdfOutputStream));
      pdfDoc = new Document(pdfDocument);

      if (paragraphCount == 0) {
        log.warn("No paragraphs found in DOC, creating empty PDF");
        pdfDoc.add(new Paragraph(""));
      } else {
        for (int i = 0; i < paragraphCount; i++) {
          String paragraphText = range.getParagraph(i).text();
          if (paragraphText != null && !paragraphText.trim().isEmpty()) {
            pdfDoc.add(new Paragraph(paragraphText));
          }
        }
      }

      pdfDoc.close();
      byte[] result = pdfOutputStream.toByteArray();
      log.debug("Successfully converted DOC to PDF, output size: " + result.length);
      return result;
    } catch (Exception e) {
      throw new IOException("Error converting DOC to PDF", e);
    } finally {
      if (pdfDoc != null) {
        try {
          pdfDoc.close();
        } catch (Exception e) {
          log.warn("Error closing PDF document", e);
        }
      }
      if (pdfDocument != null) {
        try {
          pdfDocument.close();
        } catch (Exception e) {
          log.warn("Error closing PDF document", e);
        }
      }
    }
  }
}
