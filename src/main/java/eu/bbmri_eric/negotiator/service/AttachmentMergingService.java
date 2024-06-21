package eu.bbmri_eric.negotiator.service;

import eu.bbmri_eric.negotiator.dto.attachments.AttachmentDTO;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;

@Service
public class AttachmentMergingService {

  private final AttachmentService attachmentService;

  public AttachmentMergingService(AttachmentService attachmentService) {
    this.attachmentService = attachmentService;
  }

  /**
   * Merge attachments to a PDF file.
   *
   * @param attachmentIds The list of attachment IDs to merge.
   * @return The merged PDF file.
   */
  public byte[] mergeAttachmentsToPdf(List<String> attachmentIds) {
    List<AttachmentDTO> attachmentsList =
        attachmentIds.stream()
            .map(id -> attachmentService.findById(id))
            .collect(Collectors.toList());

    List<byte[]> convertedAttachments = convertAttachmentsToPdf(attachmentsList);
    byte[] mergedPdf = null;
    try{
      mergedPdf = PdfMergerService.mergePdfs(convertedAttachments);
    } catch (IOException e) {
      throw new RuntimeException("Failed to merge attachments to PDF", e);
    }

    return mergedPdf;
  }

  private List<byte[]> convertAttachmentsToPdf(List<AttachmentDTO> attachmentsList) {
    return attachmentsList.stream()
        .map(
            attachmentDTO -> {
              try {
                if (attachmentDTO.getContentType().equals("application/pdf")) {
                  return attachmentDTO.getPayload();
                } else if (attachmentDTO
                    .getContentType()
                    .equals(
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document")) {
                  return convertDocxToPdf(attachmentDTO.getPayload());
                } else if (attachmentDTO.getContentType().equals("application/msword")) {
                  return convertDocToPdf(attachmentDTO.getPayload());
                } else {
                  return null;
                }
              } catch (IOException e) {
                e.printStackTrace();
                return null;
              }
            })
        .filter(pdfBytes -> pdfBytes != null)
        .collect(Collectors.toList());
  }

  private byte[] convertDocToPdf(byte[] docBytes) throws IOException {
    try (HWPFDocument doc = new HWPFDocument(new ByteArrayInputStream(docBytes));
        ByteArrayOutputStream pdfOutputStream = new ByteArrayOutputStream();
        PDDocument pdfDoc = new PDDocument()) {

      PDPage page = new PDPage();
      pdfDoc.addPage(page);

      try (PDPageContentStream contentStream = new PDPageContentStream(pdfDoc, page);
          WordExtractor extractor = new WordExtractor(doc)) {

        contentStream.beginText();
        contentStream.setFont(PDType1Font.HELVETICA, 12);
        contentStream.setLeading(14.5f);
        contentStream.newLineAtOffset(25, 725);

        for (String paragraph : extractor.getParagraphText()) {
          contentStream.showText(paragraph.trim());
          contentStream.newLine();
        }

        contentStream.endText();
      }

      pdfDoc.save(pdfOutputStream);
      return pdfOutputStream.toByteArray();
    }
  }

  private byte[] convertDocxToPdf(byte[] docxBytes) throws IOException {
    try (XWPFDocument docx = new XWPFDocument(new ByteArrayInputStream(docxBytes));
        ByteArrayOutputStream pdfOutputStream = new ByteArrayOutputStream();
        PDDocument pdfDoc = new PDDocument()) {

      PDPage page = new PDPage();
      pdfDoc.addPage(page);

      try (PDPageContentStream contentStream = new PDPageContentStream(pdfDoc, page)) {
        contentStream.beginText();
        contentStream.setFont(PDType1Font.HELVETICA, 12);
        contentStream.setLeading(14.5f);
        contentStream.newLineAtOffset(25, 725);

        docx.getParagraphs()
            .forEach(
                paragraph -> {
                  try {
                    contentStream.showText(paragraph.getText());
                    contentStream.newLine();
                  } catch (IOException e) {
                    e.printStackTrace();
                  }
                });

        contentStream.endText();
      }

      pdfDoc.save(pdfOutputStream);
      return pdfOutputStream.toByteArray();
    }
  }
}
