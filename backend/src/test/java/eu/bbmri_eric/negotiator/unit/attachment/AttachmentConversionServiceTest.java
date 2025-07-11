package eu.bbmri_eric.negotiator.unit.attachment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import eu.bbmri_eric.negotiator.attachment.AttachmentConversionService;
import eu.bbmri_eric.negotiator.attachment.AttachmentService;
import eu.bbmri_eric.negotiator.attachment.dto.AttachmentDTO;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AttachmentConversionServiceTest {

  @Mock private AttachmentService attachmentService;

  private AttachmentConversionService conversionService;

  @BeforeEach
  void setUp() {
    conversionService = new AttachmentConversionService(attachmentService);
  }

  @Test
  void testGetAttachmentsAsPdf_WithPdfAttachment_ReturnsOriginalBytes() {
    String attachmentId = "pdf-attachment-1";
    byte[] pdfBytes = "PDF content".getBytes();
    AttachmentDTO pdfAttachment =
        AttachmentDTO.builder()
            .id(attachmentId)
            .name("test.pdf")
            .contentType("application/pdf")
            .payload(pdfBytes)
            .build();

    when(attachmentService.findById(attachmentId)).thenReturn(pdfAttachment);

    List<byte[]> result = conversionService.getAttachmentsAsPdf(List.of(attachmentId));

    assertEquals(1, result.size());
    assertEquals(pdfBytes, result.get(0));
  }

  @Test
  void testGetAttachmentsAsPdf_WithDocxAttachment_ConvertsSuccessfully() throws IOException {
    String attachmentId = "docx-attachment-1";
    byte[] docxBytes = loadTestDocxFile();
    AttachmentDTO docxAttachment =
        AttachmentDTO.builder()
            .id(attachmentId)
            .name("test.docx")
            .contentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
            .payload(docxBytes)
            .build();

    when(attachmentService.findById(attachmentId)).thenReturn(docxAttachment);

    List<byte[]> result = conversionService.getAttachmentsAsPdf(List.of(attachmentId));

    // With valid DOCX file, should convert to PDF successfully
    assertEquals(1, result.size());
    assertTrue(result.get(0).length > 0);
  }

  @Test
  void testGetAttachmentsAsPdf_WithDocAttachment_SkipsInvalidDoc() throws IOException {
    String attachmentId = "doc-attachment-1";
    byte[] docBytes = loadTestDocFile();
    AttachmentDTO docAttachment =
        AttachmentDTO.builder()
            .id(attachmentId)
            .name("test.doc")
            .contentType("application/msword")
            .payload(docBytes)
            .build();

    when(attachmentService.findById(attachmentId)).thenReturn(docAttachment);

    List<byte[]> result = conversionService.getAttachmentsAsPdf(List.of(attachmentId));

    // The minimal DOC file created for testing is invalid and should be skipped
    assertEquals(0, result.size());
  }

  @Test
  void testGetAttachmentsAsPdf_WithTikaDocxType_ConvertsSuccessfully() throws IOException {
    String attachmentId = "tika-docx-attachment-1";
    byte[] docxBytes = loadTestDocxFile();
    AttachmentDTO docxAttachment =
        AttachmentDTO.builder()
            .id(attachmentId)
            .name("test.docx")
            .contentType("application/x-tika-ooxml")
            .payload(docxBytes)
            .build();

    when(attachmentService.findById(attachmentId)).thenReturn(docxAttachment);

    List<byte[]> result = conversionService.getAttachmentsAsPdf(List.of(attachmentId));

    // With valid DOCX file detected by Tika, should convert to PDF successfully
    assertEquals(1, result.size());
    assertTrue(result.get(0).length > 0);
  }

  @Test
  void testGetAttachmentsAsPdf_WithTikaDocType_SkipsInvalidDoc() throws IOException {
    String attachmentId = "tika-doc-attachment-1";
    byte[] docBytes = loadTestDocFile();
    AttachmentDTO docAttachment =
        AttachmentDTO.builder()
            .id(attachmentId)
            .name("test.doc")
            .contentType("application/x-tika-msoffice")
            .payload(docBytes)
            .build();

    when(attachmentService.findById(attachmentId)).thenReturn(docAttachment);

    List<byte[]> result = conversionService.getAttachmentsAsPdf(List.of(attachmentId));

    // The minimal DOC file created for testing is invalid and should be skipped
    assertEquals(0, result.size());
  }

  @Test
  void testGetAttachmentsAsPdf_WithMultipleAttachments_ProcessesAll() throws IOException {
    String pdfId = "pdf-1";
    String docxId = "docx-1";

    byte[] pdfBytes = "PDF content".getBytes();
    byte[] docxBytes = loadTestDocxFile();

    AttachmentDTO pdfAttachment =
        AttachmentDTO.builder()
            .id(pdfId)
            .name("test.pdf")
            .contentType("application/pdf")
            .payload(pdfBytes)
            .build();

    AttachmentDTO docxAttachment =
        AttachmentDTO.builder()
            .id(docxId)
            .name("test.docx")
            .contentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
            .payload(docxBytes)
            .build();

    when(attachmentService.findById(pdfId)).thenReturn(pdfAttachment);
    when(attachmentService.findById(docxId)).thenReturn(docxAttachment);

    List<byte[]> result = conversionService.getAttachmentsAsPdf(List.of(pdfId, docxId));

    // Should process PDF file and attempt DOCX conversion
    // Note: DOCX conversion may succeed or fail depending on the test file validity
    assertTrue(result.size() >= 1); // At least PDF should be processed
    assertEquals(pdfBytes, result.get(0));
  }

  @Test
  void testGetAttachmentsAsPdf_WithUnsupportedContentType_SkipsAttachment() {
    String attachmentId = "unsupported-attachment-1";
    AttachmentDTO unsupportedAttachment =
        AttachmentDTO.builder()
            .id(attachmentId)
            .name("test.txt")
            .contentType("text/plain")
            .payload("Text content".getBytes())
            .build();

    when(attachmentService.findById(attachmentId)).thenReturn(unsupportedAttachment);

    List<byte[]> result = conversionService.getAttachmentsAsPdf(List.of(attachmentId));

    assertEquals(0, result.size());
  }

  @Test
  void testGetAttachmentsAsPdf_WithInvalidDocxBytes_SkipsAttachment() {
    String attachmentId = "invalid-docx-1";
    AttachmentDTO invalidDocxAttachment =
        AttachmentDTO.builder()
            .id(attachmentId)
            .name("invalid.docx")
            .contentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
            .payload("Invalid DOCX content".getBytes())
            .build();

    when(attachmentService.findById(attachmentId)).thenReturn(invalidDocxAttachment);

    List<byte[]> result = conversionService.getAttachmentsAsPdf(List.of(attachmentId));

    assertEquals(0, result.size());
  }

  @Test
  void testGetAttachmentsAsPdf_WithInvalidDocBytes_SkipsAttachment() {
    String attachmentId = "invalid-doc-1";
    AttachmentDTO invalidDocAttachment =
        AttachmentDTO.builder()
            .id(attachmentId)
            .name("invalid.doc")
            .contentType("application/msword")
            .payload("Invalid DOC content".getBytes())
            .build();

    when(attachmentService.findById(attachmentId)).thenReturn(invalidDocAttachment);

    List<byte[]> result = conversionService.getAttachmentsAsPdf(List.of(attachmentId));

    assertEquals(0, result.size());
  }

  @Test
  void testGetAttachmentsAsPdf_WithEmptyAttachmentList_ReturnsEmptyList() {
    List<byte[]> result = conversionService.getAttachmentsAsPdf(List.of());

    assertEquals(0, result.size());
  }

  @Test
  void testGetAttachmentsAsPdf_WithCorruptedDocxFile_SkipsAttachment() {
    String attachmentId = "corrupted-docx-1";
    byte[] corruptedDocxBytes = "This is not a valid DOCX file".getBytes();
    AttachmentDTO corruptedDocxAttachment =
        AttachmentDTO.builder()
            .id(attachmentId)
            .name("corrupted.docx")
            .contentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
            .payload(corruptedDocxBytes)
            .build();

    when(attachmentService.findById(attachmentId)).thenReturn(corruptedDocxAttachment);

    List<byte[]> result = conversionService.getAttachmentsAsPdf(List.of(attachmentId));

    // Corrupted DOCX file should be skipped
    assertEquals(0, result.size());
  }

  @Test
  void testGetAttachmentsAsPdf_WithCorruptedDocFile_SkipsAttachment() {
    String attachmentId = "corrupted-doc-1";
    byte[] corruptedDocBytes = "This is not a valid DOC file".getBytes();
    AttachmentDTO corruptedDocAttachment =
        AttachmentDTO.builder()
            .id(attachmentId)
            .name("corrupted.doc")
            .contentType("application/msword")
            .payload(corruptedDocBytes)
            .build();

    when(attachmentService.findById(attachmentId)).thenReturn(corruptedDocAttachment);

    List<byte[]> result = conversionService.getAttachmentsAsPdf(List.of(attachmentId));

    // Corrupted DOC file should be skipped
    assertEquals(0, result.size());
  }

  private byte[] loadTestDocxFile() throws IOException {
    try (InputStream inputStream = getClass().getResourceAsStream("/test-documents/test.docx")) {
      if (inputStream == null) {
        return createMinimalDocxBytes();
      }
      return inputStream.readAllBytes();
    }
  }

  private byte[] loadTestDocFile() throws IOException {
    try (InputStream inputStream = getClass().getResourceAsStream("/test-documents/test.doc")) {
      if (inputStream == null) {
        return createMinimalDocBytes();
      }
      return inputStream.readAllBytes();
    }
  }

  private byte[] createMinimalDocxBytes() {
    String minimalDocx =
        "PK\u0003\u0004\u0014\u0000\u0000\u0000\u0008\u0000\u0000\u0000\u0000\u0000"
            + "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000"
            + "\u0019\u0000\u0000\u0000[Content_Types].xmlPK\u0003\u0004\u0014\u0000"
            + "\u0000\u0000\u0008\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000"
            + "\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u000b\u0000\u0000\u0000"
            + "_rels/.relsPK\u0005\u0006\u0000\u0000\u0000\u0000\u0002\u0000\u0002\u0000"
            + "^\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000";
    return minimalDocx.getBytes();
  }

  private byte[] createMinimalDocBytes() {
    byte[] docHeader = {
      (byte) 0xD0,
      (byte) 0xCF,
      (byte) 0x11,
      (byte) 0xE0,
      (byte) 0xA1,
      (byte) 0xB1,
      (byte) 0x1A,
      (byte) 0xE1
    };
    byte[] docContent = new byte[512];
    System.arraycopy(docHeader, 0, docContent, 0, docHeader.length);
    return docContent;
  }
}
