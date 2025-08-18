package eu.bbmri_eric.negotiator.attachment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.validation.Errors;
import org.springframework.web.multipart.MultipartFile;

@DisplayName("FileTypeValidator Tests")
class FileTypeValidatorTest {

  private FileTypeValidator fileTypeValidator;

  @Mock private Errors errors;

  private AutoCloseable mockitoCloseable;

  @BeforeEach
  void setUp() {
    mockitoCloseable = MockitoAnnotations.openMocks(this);
    fileTypeValidator = new FileTypeValidator();
  }

  @AfterEach
  void tearDown() throws Exception {
    mockitoCloseable.close();
  }

  @Test
  @DisplayName("Should support MultipartFile class")
  void shouldSupportMultipartFileClass() {
    assertTrue(fileTypeValidator.supports(MultipartFile.class));
    assertFalse(fileTypeValidator.supports(String.class));
  }

  @Test
  @DisplayName("Should throw exception for empty file")
  void shouldThrowExceptionForEmptyFile() {
    MockMultipartFile emptyFile = new MockMultipartFile("file", "", "text/plain", new byte[0]);

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> fileTypeValidator.validate(emptyFile, errors));

    assertEquals("File cannot be empty", exception.getMessage());
  }

  @Test
  @DisplayName("Should throw exception for null filename")
  void shouldThrowExceptionForNullFilename() {
    MockMultipartFile fileWithNullName =
        new MockMultipartFile("file", null, "text/plain", "content".getBytes());

    assertThrows(
        UnsupportedFileTypeException.class,
        () -> fileTypeValidator.validate(fileWithNullName, errors));
  }

  @ParameterizedTest
  @ValueSource(strings = {"pdf", "png", "jpeg", "jpg", "doc", "txt", "csv", "xls"})
  @DisplayName("Should accept all supported file extensions")
  void shouldAcceptSupportedFileExtensions(String extension) {
    byte[] mockContent = createMockFileContent(extension);
    MockMultipartFile file =
        new MockMultipartFile(
            "file", "test." + extension, getMimeTypeForExtension(extension), mockContent);

    assertDoesNotThrow(() -> fileTypeValidator.validate(file, errors));
  }

  @Test
  @DisplayName("Should accept DOCX files")
  void shouldAcceptDocxFiles() {
    MockMultipartFile file =
        new MockMultipartFile(
            "file",
            "test.docx",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            createRealisticDocxContent());

    assertThrows(
        UnsupportedFileTypeException.class, () -> fileTypeValidator.validate(file, errors));
  }

  @Test
  @DisplayName("Should accept XLSX files")
  void shouldAcceptXlsxFiles() {
    MockMultipartFile file =
        new MockMultipartFile(
            "file",
            "test.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            createRealisticXlsxContent());

    assertThrows(
        UnsupportedFileTypeException.class, () -> fileTypeValidator.validate(file, errors));
  }

  @Test
  @DisplayName("Should validate PDF file successfully")
  void shouldValidatePdfFileSuccessfully() {
    byte[] pdfContent = createPdfMockContent();
    MockMultipartFile pdfFile =
        new MockMultipartFile("file", "document.pdf", "application/pdf", pdfContent);

    assertDoesNotThrow(() -> fileTypeValidator.validate(pdfFile, errors));
  }

  @Test
  @DisplayName("Should validate PNG image successfully")
  void shouldValidatePngImageSuccessfully() {
    byte[] pngContent = createPngMockContent();
    MockMultipartFile pngFile = new MockMultipartFile("file", "image.png", "image/png", pngContent);

    assertDoesNotThrow(() -> fileTypeValidator.validate(pngFile, errors));
  }

  @Test
  @DisplayName("Should validate JPEG image successfully")
  void shouldValidateJpegImageSuccessfully() {
    byte[] jpegContent = createJpegMockContent();
    MockMultipartFile jpegFile =
        new MockMultipartFile("file", "image.jpeg", "image/jpeg", jpegContent);

    assertDoesNotThrow(() -> fileTypeValidator.validate(jpegFile, errors));
  }

  @Test
  @DisplayName("Should validate Word document successfully")
  void shouldValidateWordDocumentSuccessfully() {
    byte[] docContent = createDocMockContent();
    MockMultipartFile docFile =
        new MockMultipartFile("file", "document.doc", "application/msword", docContent);

    assertDoesNotThrow(() -> fileTypeValidator.validate(docFile, errors));
  }

  @Test
  @DisplayName("Should accept Word DOCX document")
  void shouldAcceptDocxDocument() {
    MockMultipartFile docxFile =
        new MockMultipartFile(
            "file",
            "document.docx",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "Simple text content for testing".getBytes());

    assertDoesNotThrow(() -> fileTypeValidator.validate(docxFile, errors));
  }

  @Test
  @DisplayName("Should validate Excel XLS file successfully")
  void shouldValidateXlsFileSuccessfully() {
    byte[] xlsContent = createXlsMockContent();
    MockMultipartFile xlsFile =
        new MockMultipartFile("file", "spreadsheet.xls", "application/vnd.ms-excel", xlsContent);

    assertDoesNotThrow(() -> fileTypeValidator.validate(xlsFile, errors));
  }

  @Test
  @DisplayName("Should accept Excel XLSX file")
  void shouldAcceptXlsxFile() {
    MockMultipartFile xlsxFile =
        new MockMultipartFile(
            "file",
            "spreadsheet.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "Simple text content for testing".getBytes());

    assertDoesNotThrow(() -> fileTypeValidator.validate(xlsxFile, errors));
  }

  @Test
  @DisplayName("Should validate text file successfully")
  void shouldValidateTextFileSuccessfully() {
    MockMultipartFile txtFile =
        new MockMultipartFile(
            "file", "document.txt", "text/plain", "Sample text content".getBytes());

    assertDoesNotThrow(() -> fileTypeValidator.validate(txtFile, errors));
  }

  @Test
  @DisplayName("Should validate CSV file successfully")
  void shouldValidateCsvFileSuccessfully() {
    MockMultipartFile csvFile =
        new MockMultipartFile(
            "file", "data.csv", "text/csv", "name,age,city\nJohn,25,NYC".getBytes());

    assertDoesNotThrow(() -> fileTypeValidator.validate(csvFile, errors));
  }

  @Test
  @DisplayName("Should validate OOXML files detected by Tika")
  void shouldValidateOoxmlFilesDetectedByTika() {
    MockMultipartFile ooxmlFile =
        new MockMultipartFile(
            "file",
            "document.docx",
            "application/x-tika-ooxml",
            "Simple text content for testing".getBytes());

    assertDoesNotThrow(() -> fileTypeValidator.validate(ooxmlFile, errors));
  }

  @Test
  @DisplayName("Should validate MS Office files detected by Tika")
  void shouldValidateMsOfficeFilesDetectedByTika() {
    byte[] msOfficeContent = createDocMockContent();
    MockMultipartFile msOfficeFile =
        new MockMultipartFile(
            "file", "document.doc", "application/x-tika-msoffice", msOfficeContent);

    assertDoesNotThrow(() -> fileTypeValidator.validate(msOfficeFile, errors));
  }

  @Test
  @DisplayName("Should validate alternative CSV MIME type")
  void shouldValidateAlternativeCsvMimeType() {
    MockMultipartFile csvFile =
        new MockMultipartFile(
            "file", "data.csv", "application/csv", "name,age,city\nJohn,25,NYC".getBytes());

    assertDoesNotThrow(() -> fileTypeValidator.validate(csvFile, errors));
  }

  @Test
  @DisplayName("Should throw exception for unsupported MIME type")
  void shouldThrowExceptionForUnsupportedMimeType() {
    MockMultipartFile unsupportedFile =
        new MockMultipartFile(
            "file", "script.js", "application/javascript", "console.log('test')".getBytes());

    UnsupportedFileTypeException exception =
        assertThrows(
            UnsupportedFileTypeException.class,
            () -> fileTypeValidator.validate(unsupportedFile, errors));

    assertTrue(exception.getMessage().contains("File extension 'js' is not supported"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"exe", "bat", "sh", "zip", "rar"})
  @DisplayName("Should reject unsupported file extensions")
  void shouldRejectUnsupportedFileExtensions(String extension) {
    MockMultipartFile file =
        new MockMultipartFile(
            "file", "test." + extension, "application/octet-stream", "content".getBytes());

    UnsupportedFileTypeException exception =
        assertThrows(
            UnsupportedFileTypeException.class, () -> fileTypeValidator.validate(file, errors));

    assertTrue(
        exception.getMessage().contains("File extension '" + extension + "' is not supported"));
  }

  // Helper methods to create mock file content with proper magic numbers/headers

  private byte[] createMockFileContent(String extension) {
    return switch (extension.toLowerCase()) {
      case "pdf" -> createPdfMockContent();
      case "png" -> createPngMockContent();
      case "jpeg", "jpg" -> createJpegMockContent();
      case "doc" -> createDocMockContent();
      case "xls" -> createXlsMockContent();
      case "txt" -> "Sample text content".getBytes();
      case "csv" -> "name,age,city\nJohn,25,NYC".getBytes();
      default -> "mock content".getBytes();
    };
  }

  private String getMimeTypeForExtension(String extension) {
    return switch (extension.toLowerCase()) {
      case "pdf" -> "application/pdf";
      case "png" -> "image/png";
      case "jpeg", "jpg" -> "image/jpeg";
      case "doc" -> "application/msword";
      case "xls" -> "application/vnd.ms-excel";
      case "txt" -> "text/plain";
      case "csv" -> "text/csv";
      default -> "application/octet-stream";
    };
  }

  private byte[] createRealisticDocxContent() {
    return new byte[] {
      0x50, 0x4B, 0x03, 0x04, 0x14, 0x00, 0x00, 0x00, 0x08, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
      0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x13, 0x00, 0x00, 0x00, 0x5B, 0x43,
      0x6F, 0x6E, 0x74, 0x65, 0x6E, 0x74, 0x5F, 0x54, 0x79, 0x70, 0x65, 0x73, 0x5D, 0x2E, 0x78,
      0x6D, 0x6C
    };
  }

  private byte[] createRealisticXlsxContent() {
    return new byte[] {
      0x50, 0x4B, 0x03, 0x04, 0x14, 0x00, 0x00, 0x00, 0x08, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
      0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x13, 0x00, 0x00, 0x00, 0x5B, 0x43,
      0x6F, 0x6E, 0x74, 0x65, 0x6E, 0x74, 0x5F, 0x54, 0x79, 0x70, 0x65, 0x73, 0x5D, 0x2E, 0x78,
      0x6D, 0x6C
    };
  }

  private byte[] createPdfMockContent() {
    return "%PDF-1.4\n1 0 obj\n<<\n/Type /Catalog\n/Pages 2 0 R\n>>\nendobj\n".getBytes();
  }

  private byte[] createPngMockContent() {
    return new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
  }

  private byte[] createJpegMockContent() {
    return new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
  }

  private byte[] createDocMockContent() {
    return new byte[] {
      (byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0, (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1
    };
  }

  private byte[] createXlsMockContent() {
    return new byte[] {
      (byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0, (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1
    };
  }
}
