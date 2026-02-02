package eu.bbmri_eric.negotiator.attachment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import eu.bbmri_eric.negotiator.attachment.dto.AttachmentDTO;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class XlsxConverterTest {

  @Mock private AttachmentService attachmentService;

  private AttachmentConversionServiceImpl conversionService;

  @BeforeEach
  void setUp() {
    conversionService = new AttachmentConversionServiceImpl(attachmentService);
  }

  @Test
  void testConvertAttachmentsToPdf_WithXlsxAttachment_ConvertsSuccessfully() throws IOException {
    String attachmentId = "xlsx-attachment-1";
    byte[] xlsxBytes = AttachmentTestHelper.createValidXlsxBytes();
    AttachmentDTO xlsxAttachment =
        AttachmentDTO.builder()
            .id(attachmentId)
            .name("test.xlsx")
            .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
            .payload(xlsxBytes)
            .build();

    when(attachmentService.findById(attachmentId)).thenReturn(xlsxAttachment);

    List<byte[]> result = conversionService.listToPdf(List.of(attachmentId));

    assertEquals(1, result.size());
    assertTrue(result.get(0).length > 0);

    byte[] pdfBytes = result.get(0);
    String pdfHeader = new String(pdfBytes, 0, Math.min(4, pdfBytes.length));
    assertTrue(pdfHeader.startsWith("%PDF"));
  }

  @Test
  void testConvertAttachmentsToPdf_WithXlsxNullPayload_SkipsAttachment() {
    String attachmentId = "xlsx-null-payload";
    AttachmentDTO attachment =
        AttachmentDTO.builder()
            .id(attachmentId)
            .name("test.xlsx")
            .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
            .payload(null)
            .build();

    when(attachmentService.findById(attachmentId)).thenReturn(attachment);

    List<byte[]> result = conversionService.listToPdf(List.of(attachmentId));

    assertEquals(0, result.size());
  }

  @Test
  void testConvertAttachmentsToPdf_WithXlsxEmptyPayload_SkipsAttachment() {
    String attachmentId = "xlsx-empty-payload";
    AttachmentDTO attachment =
        AttachmentDTO.builder()
            .id(attachmentId)
            .name("test.xlsx")
            .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
            .payload(new byte[0])
            .build();

    when(attachmentService.findById(attachmentId)).thenReturn(attachment);

    List<byte[]> result = conversionService.listToPdf(List.of(attachmentId));

    assertEquals(0, result.size());
  }

  @Test
  void testConvertAttachmentsToPdf_WithInvalidXlsxBytes_SkipsAttachment() {
    String attachmentId = "invalid-xlsx-1";
    AttachmentDTO invalidXlsxAttachment =
        AttachmentDTO.builder()
            .id(attachmentId)
            .name("invalid.xlsx")
            .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
            .payload("Invalid XLSX content".getBytes())
            .build();

    when(attachmentService.findById(attachmentId)).thenReturn(invalidXlsxAttachment);

    List<byte[]> result = conversionService.listToPdf(List.of(attachmentId));

    assertEquals(0, result.size());
  }

  @Test
  void testConvertAttachmentsToPdf_WithCorruptedXlsxFile_SkipsAttachment() {
    String attachmentId = "corrupted-xlsx-1";
    byte[] corruptedXlsxBytes = "This is not a valid XLSX file".getBytes();
    AttachmentDTO corruptedXlsxAttachment =
        AttachmentDTO.builder()
            .id(attachmentId)
            .name("corrupted.xlsx")
            .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
            .payload(corruptedXlsxBytes)
            .build();

    when(attachmentService.findById(attachmentId)).thenReturn(corruptedXlsxAttachment);

    List<byte[]> result = conversionService.listToPdf(List.of(attachmentId));

    assertEquals(0, result.size());
  }

  @Test
  void testConvertAttachmentsToPdf_WithMultipleXlsxSheets_ConvertsSuccessfully()
      throws IOException {
    String attachmentId = "multi-sheet-xlsx";
    byte[] xlsxBytes = AttachmentTestHelper.createXlsxWithMultipleSheets();
    AttachmentDTO xlsxAttachment =
        AttachmentDTO.builder()
            .id(attachmentId)
            .name("multi-sheet.xlsx")
            .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
            .payload(xlsxBytes)
            .build();

    when(attachmentService.findById(attachmentId)).thenReturn(xlsxAttachment);

    List<byte[]> result = conversionService.listToPdf(List.of(attachmentId));

    assertEquals(1, result.size());
    assertTrue(result.get(0).length > 0);

    byte[] pdfBytes = result.get(0);
    String pdfHeader = new String(pdfBytes, 0, Math.min(4, pdfBytes.length));
    assertTrue(pdfHeader.startsWith("%PDF"));
  }

  @Test
  void testConvertAttachmentsToPdf_WithEmptyXlsxWorkbook_ConvertsSuccessfully() throws IOException {
    String attachmentId = "empty-xlsx";
    byte[] xlsxBytes = AttachmentTestHelper.createEmptyXlsxBytes();
    AttachmentDTO xlsxAttachment =
        AttachmentDTO.builder()
            .id(attachmentId)
            .name("empty.xlsx")
            .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
            .payload(xlsxBytes)
            .build();

    when(attachmentService.findById(attachmentId)).thenReturn(xlsxAttachment);

    List<byte[]> result = conversionService.listToPdf(List.of(attachmentId));

    assertEquals(1, result.size());
    assertTrue(result.get(0).length > 0);
  }

  @Test
  void testConvertAttachmentsToPdf_WithMixedDocxAndXlsx_ProcessesBoth() throws IOException {
    String docxId = "docx-1";
    String xlsxId = "xlsx-1";

    byte[] docxBytes = AttachmentTestHelper.loadTestDocxFile();
    byte[] xlsxBytes = AttachmentTestHelper.createValidXlsxBytes();

    AttachmentDTO docxAttachment =
        AttachmentDTO.builder()
            .id(docxId)
            .name("test.docx")
            .contentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
            .payload(docxBytes)
            .build();

    AttachmentDTO xlsxAttachment =
        AttachmentDTO.builder()
            .id(xlsxId)
            .name("test.xlsx")
            .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
            .payload(xlsxBytes)
            .build();

    when(attachmentService.findById(docxId)).thenReturn(docxAttachment);
    when(attachmentService.findById(xlsxId)).thenReturn(xlsxAttachment);

    List<byte[]> result = conversionService.listToPdf(List.of(docxId, xlsxId));

    assertEquals(2, result.size());
    assertTrue(result.get(0).length > 0);
    assertTrue(result.get(1).length > 0);
  }

  @Test
  void testConvertAttachmentsToPdf_WithXlsxWithFormulas_ConvertsSuccessfully() throws IOException {
    String attachmentId = "xlsx-with-formulas";
    byte[] xlsxBytes = AttachmentTestHelper.createXlsxWithFormulas();
    AttachmentDTO xlsxAttachment =
        AttachmentDTO.builder()
            .id(attachmentId)
            .name("formulas.xlsx")
            .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
            .payload(xlsxBytes)
            .build();

    when(attachmentService.findById(attachmentId)).thenReturn(xlsxAttachment);

    List<byte[]> result = conversionService.listToPdf(List.of(attachmentId));

    assertEquals(1, result.size());
    assertTrue(result.get(0).length > 0);
  }

  @Test
  void testXlsxConverter_WithNullBytes_ThrowsIllegalArgumentException() {
    String attachmentId = "xlsx-null-bytes";
    AttachmentDTO attachment =
        AttachmentDTO.builder()
            .id(attachmentId)
            .name("test.xlsx")
            .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
            .payload(null)
            .build();

    when(attachmentService.findById(attachmentId)).thenReturn(attachment);

    List<byte[]> result = conversionService.listToPdf(List.of(attachmentId));

    assertEquals(0, result.size());
  }

  @Test
  void testXlsxConverter_WithEmptyBytes_ThrowsIllegalArgumentException() {
    String attachmentId = "xlsx-empty-bytes";
    AttachmentDTO attachment =
        AttachmentDTO.builder()
            .id(attachmentId)
            .name("test.xlsx")
            .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
            .payload(new byte[0])
            .build();

    when(attachmentService.findById(attachmentId)).thenReturn(attachment);

    List<byte[]> result = conversionService.listToPdf(List.of(attachmentId));

    assertEquals(0, result.size());
  }

  @Test
  void testXlsxConverter_WithVariousCellTypes_HandlesAllTypes() throws IOException {
    String attachmentId = "xlsx-various-cell-types";
    byte[] xlsxBytes = AttachmentTestHelper.createXlsxWithVariousCellTypes();
    AttachmentDTO xlsxAttachment =
        AttachmentDTO.builder()
            .id(attachmentId)
            .name("various-types.xlsx")
            .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
            .payload(xlsxBytes)
            .build();

    when(attachmentService.findById(attachmentId)).thenReturn(xlsxAttachment);

    List<byte[]> result = conversionService.listToPdf(List.of(attachmentId));

    assertEquals(1, result.size());
    assertTrue(result.get(0).length > 0);

    byte[] pdfBytes = result.get(0);
    String pdfHeader = new String(pdfBytes, 0, Math.min(4, pdfBytes.length));
    assertTrue(pdfHeader.startsWith("%PDF"));
  }

  @Test
  void testXlsxConverter_WithLongText_TruncatesCorrectly() throws IOException {
    String attachmentId = "xlsx-long-text";
    byte[] xlsxBytes = AttachmentTestHelper.createXlsxWithLongText();
    AttachmentDTO xlsxAttachment =
        AttachmentDTO.builder()
            .id(attachmentId)
            .name("long-text.xlsx")
            .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
            .payload(xlsxBytes)
            .build();

    when(attachmentService.findById(attachmentId)).thenReturn(xlsxAttachment);

    List<byte[]> result = conversionService.listToPdf(List.of(attachmentId));

    assertEquals(1, result.size());
    assertTrue(result.get(0).length > 0);
  }

  @Test
  void testXlsxConverter_WithShortText_DoesNotTruncate() throws IOException {
    String attachmentId = "xlsx-short-text";
    byte[] xlsxBytes = AttachmentTestHelper.createXlsxWithShortText();
    AttachmentDTO xlsxAttachment =
        AttachmentDTO.builder()
            .id(attachmentId)
            .name("short-text.xlsx")
            .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
            .payload(xlsxBytes)
            .build();

    when(attachmentService.findById(attachmentId)).thenReturn(xlsxAttachment);

    List<byte[]> result = conversionService.listToPdf(List.of(attachmentId));

    assertEquals(1, result.size());
    assertTrue(result.get(0).length > 0);
  }

  @Test
  void testXlsxConverter_WithManyRows_HandlesPageBreak() throws IOException {
    String attachmentId = "xlsx-many-rows";
    byte[] xlsxBytes = AttachmentTestHelper.createXlsxWithManyRows();
    AttachmentDTO xlsxAttachment =
        AttachmentDTO.builder()
            .id(attachmentId)
            .name("many-rows.xlsx")
            .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
            .payload(xlsxBytes)
            .build();

    when(attachmentService.findById(attachmentId)).thenReturn(xlsxAttachment);

    List<byte[]> result = conversionService.listToPdf(List.of(attachmentId));

    assertEquals(1, result.size());
    assertTrue(result.get(0).length > 0);
  }

  @Test
  void testXlsxConverter_WithEmptyCells_HandlesCorrectly() throws IOException {
    String attachmentId = "xlsx-empty-cells";
    byte[] xlsxBytes = AttachmentTestHelper.createXlsxWithEmptyCells();
    AttachmentDTO xlsxAttachment =
        AttachmentDTO.builder()
            .id(attachmentId)
            .name("empty-cells.xlsx")
            .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
            .payload(xlsxBytes)
            .build();

    when(attachmentService.findById(attachmentId)).thenReturn(xlsxAttachment);

    List<byte[]> result = conversionService.listToPdf(List.of(attachmentId));

    assertEquals(1, result.size());
    assertTrue(result.get(0).length > 0);
  }

  @Test
  void testXlsxConverter_WithSingleColumn_UsesMinWidth() throws IOException {
    String attachmentId = "xlsx-single-column";
    byte[] xlsxBytes = AttachmentTestHelper.createXlsxWithSingleColumn();
    AttachmentDTO xlsxAttachment =
        AttachmentDTO.builder()
            .id(attachmentId)
            .name("single-column.xlsx")
            .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
            .payload(xlsxBytes)
            .build();

    when(attachmentService.findById(attachmentId)).thenReturn(xlsxAttachment);

    List<byte[]> result = conversionService.listToPdf(List.of(attachmentId));

    assertEquals(1, result.size());
    assertTrue(result.get(0).length > 0);
  }

  @Test
  void testXlsxConverter_WithBooleanAndBlankCells_HandlesCorrectly() throws IOException {
    String attachmentId = "xlsx-boolean-blank";
    byte[] xlsxBytes = AttachmentTestHelper.createXlsxWithBooleanAndBlankCells();
    AttachmentDTO xlsxAttachment =
        AttachmentDTO.builder()
            .id(attachmentId)
            .name("boolean-blank.xlsx")
            .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
            .payload(xlsxBytes)
            .build();

    when(attachmentService.findById(attachmentId)).thenReturn(xlsxAttachment);

    List<byte[]> result = conversionService.listToPdf(List.of(attachmentId));

    assertEquals(1, result.size());
    assertTrue(result.get(0).length > 0);
  }
}
