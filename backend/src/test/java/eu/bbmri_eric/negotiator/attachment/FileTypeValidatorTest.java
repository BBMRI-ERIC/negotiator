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

    assertDoesNotThrow(() -> fileTypeValidator.validate(file, errors));
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

    assertDoesNotThrow(() -> fileTypeValidator.validate(file, errors));
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

  @Test
  @DisplayName("Should correctly identify DOCX as Office document, not ZIP")
  void shouldCorrectlyIdentifyDocxAsOfficeDocumentNotZip() {
    // Create a realistic DOCX file structure with ZIP header but Office content
    byte[] docxContent = createRealisticDocxWithZipStructure();
    MockMultipartFile docxFile =
        new MockMultipartFile(
            "file",
            "document.docx",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            docxContent);

    // Should not throw exception - Tika should detect this as Office document, not ZIP
    assertDoesNotThrow(() -> fileTypeValidator.validate(docxFile, errors));
  }

  @Test
  @DisplayName("Should correctly identify DOC as Office document, not ZIP")
  void shouldCorrectlyIdentifyDocAsOfficeDocumentNotZip() {
    // Create a realistic DOC file with compound document structure
    byte[] docContent = createRealisticDocWithCompoundStructure();
    MockMultipartFile docFile =
        new MockMultipartFile("file", "document.doc", "application/msword", docContent);

    // Should not throw exception - Tika should detect this as Office document
    assertDoesNotThrow(() -> fileTypeValidator.validate(docFile, errors));
  }

  @Test
  @DisplayName("Should accept files with Office extensions even when detected as ZIP")
  void shouldAcceptFilesWithOfficeExtensionsEvenWhenDetectedAsZip() {
    byte[] zipContent = createPlainZipContent();

    MockMultipartFile fileWithDocxExtension =
        new MockMultipartFile("file", "document.docx", "application/zip", zipContent);
    assertDoesNotThrow(() -> fileTypeValidator.validate(fileWithDocxExtension, errors));
  }

  @Test
  @DisplayName("Should handle DOCX with minimal Office document structure")
  void shouldHandleDocxWithMinimalOfficeStructure() {
    byte[] minimalDocxContent = createMinimalValidDocxContent();
    MockMultipartFile docxFile =
        new MockMultipartFile(
            "file",
            "minimal.docx",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            minimalDocxContent);

    assertDoesNotThrow(() -> fileTypeValidator.validate(docxFile, errors));
  }

  @Test
  @DisplayName("Should handle XLSX with minimal Office document structure")
  void shouldHandleXlsxWithMinimalOfficeStructure() {
    // Create minimal but valid XLSX structure that Tika can identify
    byte[] minimalXlsxContent = createMinimalValidXlsxContent();
    MockMultipartFile xlsxFile =
        new MockMultipartFile(
            "file",
            "minimal.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            minimalXlsxContent);

    assertDoesNotThrow(() -> fileTypeValidator.validate(xlsxFile, errors));
  }

  @Test
  @DisplayName("Should detect Tika generic OOXML classification for Office documents")
  void shouldDetectTikaGenericOoxmlClassification() {
    // Test case where Tika detects as generic OOXML instead of specific Office type
    byte[] ooxmlContent = createRealisticDocxWithZipStructure();
    MockMultipartFile ooxmlFile =
        new MockMultipartFile(
            "file",
            "document.docx",
            "application/x-tika-ooxml", // Tika's generic OOXML detection
            ooxmlContent);

    assertDoesNotThrow(() -> fileTypeValidator.validate(ooxmlFile, errors));
  }

  @Test
  @DisplayName("Should detect Tika generic MS Office classification for legacy Office documents")
  void shouldDetectTikaGenericMsOfficeClassification() {
    // Test case where Tika detects as generic MS Office instead of specific type
    byte[] msOfficeContent = createRealisticDocWithCompoundStructure();
    MockMultipartFile msOfficeFile =
        new MockMultipartFile(
            "file",
            "document.doc",
            "application/x-tika-msoffice", // Tika's generic MS Office detection
            msOfficeContent);

    assertDoesNotThrow(() -> fileTypeValidator.validate(msOfficeFile, errors));
  }

  private byte[] createRealisticDocxWithZipStructure() {
    // Create a DOCX-like structure with proper ZIP header and Office document markers
    // This mimics the actual structure of a DOCX file (ZIP container with Office XML)
    return new byte[] {
      // ZIP file signature
      0x50,
      0x4B,
      0x03,
      0x04,
      // ZIP version
      0x14,
      0x00,
      0x00,
      0x00,
      // Compression method, flags, etc.
      0x08,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      // Filename length
      0x13,
      0x00,
      0x00,
      0x00,
      // Office document marker - [Content_Types].xml file
      0x5B,
      0x43,
      0x6F,
      0x6E,
      0x74,
      0x65,
      0x6E,
      0x74,
      0x5F,
      0x54,
      0x79,
      0x70,
      0x65,
      0x73,
      0x5D,
      0x2E,
      0x78,
      0x6D,
      0x6C,
      // Add some XML content that identifies this as an Office document
      0x3C,
      0x3F,
      0x78,
      0x6D,
      0x6C,
      0x20,
      0x76,
      0x65,
      0x72,
      0x73,
      0x69,
      0x6F,
      0x6E,
      0x3D,
      0x22,
      0x31,
      0x2E,
      0x30,
      0x22,
      // More Office-specific markers
      0x20,
      0x65,
      0x6E,
      0x63,
      0x6F,
      0x64,
      0x69,
      0x6E,
      0x67,
      0x3D,
      0x22,
      0x55,
      0x54,
      0x46,
      0x2D,
      0x38,
      0x22,
      0x3F,
      0x3E
    };
  }

  private byte[] createRealisticDocWithCompoundStructure() {
    // Create a DOC-like structure with compound document header
    return new byte[] {
      // Compound Document signature (OLE2 signature)
      (byte) 0xD0,
      (byte) 0xCF,
      0x11,
      (byte) 0xE0,
      (byte) 0xA1,
      (byte) 0xB1,
      0x1A,
      (byte) 0xE1,
      // Add more compound document structure
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      // Minor version, major version, byte order identifier
      0x3E,
      0x00,
      0x03,
      0x00,
      (byte) 0xFE,
      (byte) 0xFF,
      // Sector size, mini sector size
      0x09,
      0x00,
      0x06,
      0x00,
      // Additional compound document markers that identify this as MS Office
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x10,
      0x00,
      0x00
    };
  }

  private byte[] createPlainZipContent() {
    // Create a plain ZIP file without Office document markers
    return new byte[] {
      // ZIP file signature
      0x50,
      0x4B,
      0x03,
      0x04,
      // ZIP version and other headers
      0x14,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      // Generic filename (not Office-related)
      0x08,
      0x00,
      0x00,
      0x00,
      0x74,
      0x65,
      0x73,
      0x74,
      0x2E,
      0x74,
      0x78,
      0x74, // "test.txt"
      // Generic content
      0x74,
      0x65,
      0x73,
      0x74,
      0x20,
      0x63,
      0x6F,
      0x6E,
      0x74,
      0x65,
      0x6E,
      0x74 // "test content"
    };
  }

  private byte[] createMinimalValidDocxContent() {
    // Minimal DOCX structure that Tika can identify as Office document
    return new byte[] {
      // ZIP signature
      0x50,
      0x4B,
      0x03,
      0x04,
      0x14,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      // Content_Types file (essential for Office document identification)
      0x13,
      0x00,
      0x00,
      0x00,
      0x5B,
      0x43,
      0x6F,
      0x6E,
      0x74,
      0x65,
      0x6E,
      0x74,
      0x5F,
      0x54,
      0x79,
      0x70,
      0x65,
      0x73,
      0x5D,
      0x2E,
      0x78,
      0x6D,
      0x6C,
      // Minimal XML content with Office namespace
      0x3C,
      0x54,
      0x79,
      0x70,
      0x65,
      0x73,
      0x3E,
      0x3C,
      0x4F,
      0x76,
      0x65,
      0x72,
      0x72,
      0x69,
      0x64,
      0x65,
      0x20,
      0x43,
      0x6F,
      0x6E,
      0x74,
      0x65,
      0x6E,
      0x74,
      0x54,
      0x79,
      0x70,
      0x65,
      0x3D,
      0x22,
      0x61,
      0x70,
      0x70,
      0x6C,
      0x69,
      0x63,
      0x61,
      0x74,
      0x69,
      0x6F,
      0x6E,
      0x2F,
      0x78,
      0x6C,
      0x73,
      0x78,
      0x22,
      0x3E
    };
  }

  private byte[] createMinimalValidXlsxContent() {
    // Minimal XLSX structure that Tika can identify as Office document
    return new byte[] {
      // ZIP signature
      0x50,
      0x4B,
      0x03,
      0x04,
      0x14,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      // Content_Types file
      0x13,
      0x00,
      0x00,
      0x00,
      0x5B,
      0x43,
      0x6F,
      0x6E,
      0x74,
      0x65,
      0x6E,
      0x74,
      0x5F,
      0x54,
      0x79,
      0x70,
      0x65,
      0x73,
      0x5D,
      0x2E,
      0x78,
      0x6D,
      0x6C,
      // Minimal XML with Excel-specific content type
      0x3C,
      0x54,
      0x79,
      0x70,
      0x65,
      0x73,
      0x3E,
      0x3C,
      0x4F,
      0x76,
      0x65,
      0x72,
      0x72,
      0x69,
      0x64,
      0x65,
      0x20,
      0x43,
      0x6F,
      0x6E,
      0x74,
      0x65,
      0x6E,
      0x74,
      0x54,
      0x79,
      0x70,
      0x65,
      0x3D,
      0x22,
      0x61,
      0x70,
      0x70,
      0x6C,
      0x69,
      0x63,
      0x61,
      0x74,
      0x69,
      0x6F,
      0x6E,
      0x2F,
      0x78,
      0x6C,
      0x73,
      0x78,
      0x22,
      0x3E
    };
  }
}
