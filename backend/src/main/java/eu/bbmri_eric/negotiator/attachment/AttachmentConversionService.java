package eu.bbmri_eric.negotiator.attachment;

import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import eu.bbmri_eric.negotiator.attachment.dto.AttachmentDTO;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.usermodel.Range;
import org.docx4j.Docx4J;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.springframework.stereotype.Service;

/**
 * Service for converting attachments to PDF format.
 */
@Service
@CommonsLog
public class AttachmentConversionService {
  private final AttachmentService attachmentService;

  public AttachmentConversionService(AttachmentService attachmentService) {
    this.attachmentService = attachmentService;
  }

  /**
   * Retrieves the specified attachments and converts them to PDF format.
   *
   * @param attachmentIds the list of attachment IDs to retrieve and convert
   * @return a list of byte arrays, each representing a PDF file
   */
  public List<byte[]> getAttachmentsAsPdf(List<String> attachmentIds) {
    List<AttachmentDTO> attachmentsList =
        attachmentIds.stream()
            .map(id -> attachmentService.findById(id))
            .collect(Collectors.toList());

    return convertAttachmentsToPdf(attachmentsList);
  }

  private List<byte[]> convertAttachmentsToPdf(List<AttachmentDTO> attachmentsList) {
    return attachmentsList.stream()
        .map(
            attachmentDTO -> {
              try {
                String contentType = attachmentDTO.getContentType();

                if (contentType.equals("application/pdf")) {
                  return attachmentDTO.getPayload();
                } else if (
                    contentType.equals(
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document" // .docx
                    ) || contentType.equals("application/x-tika-ooxml") // .docx (generic Tika type)
                ) {
                  return convertDocxToPdf(attachmentDTO.getPayload());
                } else if (contentType.equals("application/msword") // .doc 
                  || contentType.equals("application/x-tika-msoffice")) { // .doc (generic Tika type)
                  return convertDocToPdf(attachmentDTO.getPayload());
                } else {
                  log.error("Unrecognized attachment content type: " + contentType);
                  return null;
                }
              } catch (Exception e) {
                log.error("Error converting attachment to PDF: " + e.getMessage());
                return null;
              }
            })
        .filter(pdfBytes -> pdfBytes != null)
        .collect(Collectors.toList());
  }

  private byte[] convertDocToPdf(byte[] docBytes) throws Exception {
    Document pdfDoc = null;

    try (ByteArrayInputStream docInputStream = new ByteArrayInputStream(docBytes);
        HWPFDocument doc = new HWPFDocument(docInputStream);
        ByteArrayOutputStream pdfOutputStream = new ByteArrayOutputStream()) {

      Range range = doc.getRange();

      pdfDoc = new Document();
      PdfWriter.getInstance(pdfDoc, pdfOutputStream);
      pdfDoc.open();

      for (int i = 0; i < range.numParagraphs(); i++) {
        pdfDoc.add(new Paragraph(range.getParagraph(i).text()));
      }
      pdfDoc.close();
      return pdfOutputStream.toByteArray();
    } finally {
      if (pdfDoc != null) {
        pdfDoc.close();
      }
    }
  }

  private byte[] convertDocxToPdf(byte[] docxBytes) throws Exception {
    if (docxBytes == null || docxBytes.length == 0) {
      throw new IllegalArgumentException("Input DOCX bytes are null or empty");
    }
    try (ByteArrayInputStream docxInputStream = new ByteArrayInputStream(docxBytes);
        ByteArrayOutputStream pdfOutputStream = new ByteArrayOutputStream()) {

      WordprocessingMLPackage wordMLPackage = Docx4J.load(docxInputStream);

      Docx4J.toPDF(wordMLPackage, pdfOutputStream);
      return pdfOutputStream.toByteArray();
    }
  }
}
