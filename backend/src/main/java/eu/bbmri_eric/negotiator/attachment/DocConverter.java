package eu.bbmri_eric.negotiator.attachment;

import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.usermodel.Range;

@CommonsLog
class DocConverter implements FileTypeConverter {
  private static final String CONTENT_TYPE_DOC = "application/msword";
  private static final String CONTENT_TYPE_TIKA_MSOFFICE = "application/x-tika-msoffice";

  @Override
  public byte[] convertToPdf(byte[] docBytes) throws IOException {
    if (docBytes == null || docBytes.length == 0) {
      throw new IllegalArgumentException("Input DOC bytes are null or empty");
    }

    log.debug("Converting DOC to PDF, input size: " + docBytes.length);
    Document pdfDoc = null;

    try (ByteArrayInputStream docInputStream = new ByteArrayInputStream(docBytes);
        HWPFDocument doc = new HWPFDocument(docInputStream);
        ByteArrayOutputStream pdfOutputStream = new ByteArrayOutputStream()) {

      Range range = doc.getRange();
      int paragraphCount = range.numParagraphs();
      log.debug("Processing paragraphs from DOC: " + paragraphCount);

      pdfDoc = new Document();
      PdfWriter.getInstance(pdfDoc, pdfOutputStream);
      pdfDoc.open();

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
    } finally {
      if (pdfDoc != null && pdfDoc.isOpen()) {
        pdfDoc.close();
      }
    }
  }

  @Override
  public boolean supports(String contentType) {
    return StringUtils.equalsAny(contentType, CONTENT_TYPE_DOC, CONTENT_TYPE_TIKA_MSOFFICE);
  }
}
