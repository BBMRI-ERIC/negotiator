package eu.bbmri_eric.negotiator.attachment;

import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import eu.bbmri_eric.negotiator.attachment.dto.AttachmentDTO;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.usermodel.Range;
import org.docx4j.Docx4J;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.springframework.stereotype.Service;

/** Service for converting attachments to PDF format. */
@Service
@CommonsLog
public class AttachmentConversionService {
  private static final String CONTENT_TYPE_PDF = "application/pdf";
  private static final String CONTENT_TYPE_DOCX =
      "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
  private static final String CONTENT_TYPE_TIKA_OOXML = "application/x-tika-ooxml";
  private static final String CONTENT_TYPE_DOC = "application/msword";
  private static final String CONTENT_TYPE_TIKA_MSOFFICE = "application/x-tika-msoffice";

  private final AttachmentService attachmentService;

  public AttachmentConversionService(AttachmentService attachmentService) {
    this.attachmentService = attachmentService;
  }

  /**
   * Retrieves the specified attachments and converts them to PDF format.
   *
   * @param attachmentIds the list of attachment IDs to retrieve and convert
   * @return a list of byte arrays, each representing a PDF file
   * @throws IllegalArgumentException if attachmentIds is null or empty
   */
  public List<byte[]> getAttachmentsAsPdf(List<String> attachmentIds) {
    if (attachmentIds == null || attachmentIds.isEmpty()) {
      log.warn("Attachment IDs list is null or empty");
      throw new IllegalArgumentException("Attachment IDs list cannot be null or empty");
    }

    log.debug("Converting {" + attachmentIds.size() + "} attachments to PDF");

    List<AttachmentDTO> attachmentsList =
        attachmentIds.stream()
            .filter(Objects::nonNull)
            .map(
                id -> {
                  try {
                    return attachmentService.findById(id);
                  } catch (Exception e) {
                    log.error("Failed to retrieve attachment with ID: {}" + id, e);
                    return null;
                  }
                })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

    if (attachmentsList.isEmpty()) {
      log.warn("No valid attachments found for conversion");
      return List.of();
    }

    return convertAttachmentsToPdf(attachmentsList);
  }

  private List<byte[]> convertAttachmentsToPdf(List<AttachmentDTO> attachmentsList) {
    return attachmentsList.stream()
        .map(this::convertSingleAttachmentToPdf)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  private byte[] convertSingleAttachmentToPdf(AttachmentDTO attachmentDTO) {
    if (attachmentDTO == null) {
      log.warn("Attachment DTO is null, skipping conversion");
      return null;
    }

    try {
      String contentType = attachmentDTO.getContentType();
      byte[] payload = attachmentDTO.getPayload();

      if (contentType == null) {
        log.error("Content type is null for attachment");
        return null;
      }

      if (payload == null || payload.length == 0) {
        log.error(
            "Payload is null or empty for attachment with content type: {" + contentType + "}");
        return null;
      }

      log.debug("Converting attachment with content type: {" + contentType + "}");

      switch (contentType) {
        case CONTENT_TYPE_PDF:
          log.debug("Attachment is already PDF, returning as-is");
          return payload;
        case CONTENT_TYPE_DOCX:
        case CONTENT_TYPE_TIKA_OOXML:
          return convertDocxToPdf(payload);
        case CONTENT_TYPE_DOC:
        case CONTENT_TYPE_TIKA_MSOFFICE:
          return convertDocToPdf(payload);
        default:
          log.error("Unrecognized attachment content type: {" + contentType + "}");
          return null;
      }
    } catch (Exception e) {
      log.error("Error converting attachment to PDF: {" + e.getMessage() + "}", e);
      return null;
    }
  }

  private byte[] convertDocToPdf(byte[] docBytes) throws Exception {
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
      log.debug("Processing {" + paragraphCount + "} paragraphs from DOC");

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

  private byte[] convertDocxToPdf(byte[] docxBytes) throws Exception {
    if (docxBytes == null || docxBytes.length == 0) {
      throw new IllegalArgumentException("Input DOCX bytes are null or empty");
    }

    log.debug("Converting DOCX to PDF, input size: " + docxBytes.length);

    try (ByteArrayInputStream docxInputStream = new ByteArrayInputStream(docxBytes);
        ByteArrayOutputStream pdfOutputStream = new ByteArrayOutputStream()) {

      WordprocessingMLPackage wordMLPackage = Docx4J.load(docxInputStream);

      if (wordMLPackage == null) {
        throw new IllegalStateException("Failed to load DOCX package");
      }

      Docx4J.toPDF(wordMLPackage, pdfOutputStream);
      byte[] result = pdfOutputStream.toByteArray();
      return result;
    }
  }
}
