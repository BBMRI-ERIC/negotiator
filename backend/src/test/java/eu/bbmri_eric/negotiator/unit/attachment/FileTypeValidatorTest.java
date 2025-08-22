package eu.bbmri_eric.negotiator.unit.attachment;

import static org.junit.jupiter.api.Assertions.*;

import eu.bbmri_eric.negotiator.attachment.FileTypeValidator;
import eu.bbmri_eric.negotiator.attachment.UnsupportedFileTypeException;
import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.multipart.MultipartFile;

class FileTypeValidatorTest {

  private FileTypeValidator validator;

  @BeforeEach
  void setUp() {
    validator = new FileTypeValidator();
  }

  @Test
  void testSupports_WithMultipartFile_ReturnsTrue() {
    assertTrue(validator.supports(MultipartFile.class));
  }

  @Test
  void testSupports_WithOtherClass_ReturnsFalse() {
    assertFalse(validator.supports(String.class));
    assertFalse(validator.supports(Object.class));
  }

  @Test
  void testValidate_WithEmptyFile_ThrowsIllegalArgumentException() {
    MockMultipartFile emptyFile = new MockMultipartFile("file", "", "text/plain", new byte[0]);
    Errors errors = new BeanPropertyBindingResult(emptyFile, "file");

    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> validator.validate(emptyFile, errors));
    assertEquals("File cannot be empty", exception.getMessage());
  }

  @Test
  void testValidate_WithValidPdfFile_PassesValidation() {
    byte[] pdfBytes = createValidPdfBytes();
    MockMultipartFile pdfFile =
        new MockMultipartFile("file", "test.pdf", "application/pdf", pdfBytes);
    Errors errors = new BeanPropertyBindingResult(pdfFile, "file");

    assertDoesNotThrow(() -> validator.validate(pdfFile, errors));
  }

  @Test
  void testValidate_WithValidTextFile_PassesValidation() {
    byte[] textBytes = "This is a test text file.".getBytes();
    MockMultipartFile textFile = new MockMultipartFile("file", "test.txt", "text/plain", textBytes);
    Errors errors = new BeanPropertyBindingResult(textFile, "file");

    assertDoesNotThrow(() -> validator.validate(textFile, errors));
  }

  @Test
  void testValidate_WithValidDocxFile_PassesValidation() {
    byte[] docxBytes = createValidDocxBytes();
    MockMultipartFile docxFile =
        new MockMultipartFile(
            "file",
            "test.docx",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            docxBytes);
    Errors errors = new BeanPropertyBindingResult(docxFile, "file");

    assertDoesNotThrow(() -> validator.validate(docxFile, errors));
  }

  @Test
  void testValidate_WithValidDocFile_PassesValidation() {
    byte[] docBytes = createValidDocBytes();
    MockMultipartFile docFile =
        new MockMultipartFile("file", "test.doc", "application/msword", docBytes);
    Errors errors = new BeanPropertyBindingResult(docFile, "file");

    assertDoesNotThrow(() -> validator.validate(docFile, errors));
  }

  @Test
  void testValidate_WithValidJpegFile_PassesValidation() {
    byte[] jpegBytes = createValidJpegBytes();
    MockMultipartFile jpegFile = new MockMultipartFile("file", "test.jpg", "image/jpeg", jpegBytes);
    Errors errors = new BeanPropertyBindingResult(jpegFile, "file");

    assertDoesNotThrow(() -> validator.validate(jpegFile, errors));
  }

  @Test
  void testValidate_WithValidPngFile_PassesValidation() {
    byte[] pngBytes = createValidPngBytes();
    MockMultipartFile pngFile = new MockMultipartFile("file", "test.png", "image/png", pngBytes);
    Errors errors = new BeanPropertyBindingResult(pngFile, "file");

    assertDoesNotThrow(() -> validator.validate(pngFile, errors));
  }

  @Test
  void testValidate_WithValidCsvFile_PassesValidation() {
    byte[] csvBytes = "Name,Age,City\nJohn,30,New York\nJane,25,London".getBytes();
    MockMultipartFile csvFile = new MockMultipartFile("file", "test.csv", "text/csv", csvBytes);
    Errors errors = new BeanPropertyBindingResult(csvFile, "file");

    assertDoesNotThrow(() -> validator.validate(csvFile, errors));
  }

  @Test
  void testValidate_WithExecutableFile_ThrowsUnsupportedFileTypeException() {
    byte[] exeBytes = createExecutableBytes();
    MockMultipartFile exeFile =
        new MockMultipartFile("file", "malicious.exe", "application/octet-stream", exeBytes);
    Errors errors = new BeanPropertyBindingResult(exeFile, "file");

    UnsupportedFileTypeException exception =
        assertThrows(UnsupportedFileTypeException.class, () -> validator.validate(exeFile, errors));
    assertTrue(exception.getMessage().contains("exe"));
    assertTrue(exception.getMessage().contains("not supported"));
  }

  @Test
  void testValidate_WithDoubleExtensionAttack_ThrowsUnsupportedFileTypeException() {
    byte[] exeBytes = createExecutableBytes();
    MockMultipartFile maliciousFile =
        new MockMultipartFile("file", "malicious.exe.pdf", "application/octet-stream", exeBytes);
    Errors errors = new BeanPropertyBindingResult(maliciousFile, "file");

    UnsupportedFileTypeException exception =
        assertThrows(
            UnsupportedFileTypeException.class, () -> validator.validate(maliciousFile, errors));
    // The validation fails on the file extension check first (pdf extension)
    assertTrue(
        exception.getMessage().contains("pdf") || exception.getMessage().contains("not supported"));
  }

  @Test
  void testValidate_WithUnsupportedMimeType_ThrowsUnsupportedFileTypeException() {
    byte[] exeBytes = createExecutableBytes();
    MockMultipartFile executableFile =
        new MockMultipartFile("file", "test.pdf", "application/pdf", exeBytes);
    Errors errors = new BeanPropertyBindingResult(executableFile, "file");

    UnsupportedFileTypeException exception =
        assertThrows(
            UnsupportedFileTypeException.class, () -> validator.validate(executableFile, errors));
    // The detected MIME type will be application/octet-stream or application/x-dosexec
    assertTrue(exception.getMessage().contains("not supported"));
  }

  @Test
  void testValidate_WithMismatchedExtensionAndMimeType_ThrowsUnsupportedFileTypeException() {
    byte[] textBytes = "This is actually text content".getBytes();
    MockMultipartFile mismatchedFile =
        new MockMultipartFile("file", "fake.exe", "application/octet-stream", textBytes);
    Errors errors = new BeanPropertyBindingResult(mismatchedFile, "file");

    UnsupportedFileTypeException exception =
        assertThrows(
            UnsupportedFileTypeException.class, () -> validator.validate(mismatchedFile, errors));
    assertTrue(exception.getMessage().contains("exe"));
    assertTrue(exception.getMessage().contains("not supported"));
  }

  @Test
  void testValidate_WithNullFilename_ThrowsUnsupportedFileTypeException() {
    byte[] textBytes = "This is text content".getBytes();
    MockMultipartFile nullNameFile = new MockMultipartFile("file", null, "text/plain", textBytes);
    Errors errors = new BeanPropertyBindingResult(nullNameFile, "file");

    UnsupportedFileTypeException exception =
        assertThrows(
            UnsupportedFileTypeException.class, () -> validator.validate(nullNameFile, errors));
    assertTrue(exception.getMessage().contains("not supported"));
  }

  @Test
  void testValidate_WithFilenameWithoutExtension_ThrowsUnsupportedFileTypeException() {
    byte[] textBytes = "This is text content".getBytes();
    MockMultipartFile noExtensionFile =
        new MockMultipartFile("file", "filename", "text/plain", textBytes);
    Errors errors = new BeanPropertyBindingResult(noExtensionFile, "file");

    UnsupportedFileTypeException exception =
        assertThrows(
            UnsupportedFileTypeException.class, () -> validator.validate(noExtensionFile, errors));
    assertTrue(exception.getMessage().contains("not supported"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"pdf", "png", "jpeg", "jpg", "doc", "docx", "txt", "csv", "xls", "xlsx"})
  void testValidate_WithAllowedExtensions_PassesValidation(String extension) {
    byte[] content = getValidContentForExtension(extension);
    String mimeType = getMimeTypeForExtension(extension);
    MockMultipartFile file = new MockMultipartFile("file", "test." + extension, mimeType, content);
    Errors errors = new BeanPropertyBindingResult(file, "file");

    assertDoesNotThrow(() -> validator.validate(file, errors));
  }

  @ParameterizedTest
  @ValueSource(strings = {"exe", "bat", "com", "scr", "vbs", "js", "jar", "zip", "rar"})
  void testValidate_WithDisallowedExtensions_ThrowsException(String extension) {
    byte[] content = createGenericBinaryContent();
    MockMultipartFile file =
        new MockMultipartFile("file", "test." + extension, "application/octet-stream", content);
    Errors errors = new BeanPropertyBindingResult(file, "file");

    UnsupportedFileTypeException exception =
        assertThrows(UnsupportedFileTypeException.class, () -> validator.validate(file, errors));
    assertTrue(exception.getMessage().contains(extension));
    assertTrue(exception.getMessage().contains("not supported"));
  }

  @ParameterizedTest
  @MethodSource("provideTikaDetectedTypes")
  void testValidate_WithTikaDetectedTypes_PassesValidation(
      String filename, String declaredMimeType, byte[] content) {
    MockMultipartFile file = new MockMultipartFile("file", filename, declaredMimeType, content);
    Errors errors = new BeanPropertyBindingResult(file, "file");

    assertDoesNotThrow(() -> validator.validate(file, errors));
  }

  @Test
  void testValidate_WithIOException_ThrowsIllegalArgumentException() {
    // Create a mock file that throws IOException on getInputStream()
    MultipartFile problematicFile =
        new MockMultipartFile("file", "test.txt", "text/plain", "content".getBytes()) {
          @Override
          public InputStream getInputStream() throws IOException {
            throw new IOException("Simulated IO error");
          }
        };

    Errors errors = new BeanPropertyBindingResult(problematicFile, "file");

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> validator.validate(problematicFile, errors));
    assertTrue(exception.getMessage().contains("Could not read file"));
    assertTrue(exception.getCause() instanceof IOException);
  }

  // Helper methods to create valid file content

  private byte[] createValidPdfBytes() {
    return ("%PDF-1.4\n1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj\n"
            + "2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj\n"
            + "3 0 obj<</Type/Page/MediaBox[0 0 612 792]/Parent 2 0 R>>endobj\n"
            + "xref\n0 4\n0000000000 65535 f \n0000000010 00000 n \n"
            + "0000000053 00000 n \n0000000125 00000 n \n"
            + "trailer<</Size 4/Root 1 0 R>>\nstartxref\n182\n%%EOF")
        .getBytes();
  }

  private byte[] createValidDocxBytes() {
    // Minimal DOCX ZIP structure
    return new byte[] {
      0x50, 0x4B, 0x03, 0x04, 0x14, 0x00, 0x00, 0x00, 0x08, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
      0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x19, 0x00, 0x00, 0x00,
      0x5B, 0x43, 0x6F, 0x6E, 0x74, 0x65, 0x6E, 0x74, 0x5F, 0x54, 0x79, 0x70, 0x65, 0x73, 0x5D,
      0x2E, 0x78, 0x6D, 0x6C, 0x50, 0x4B, 0x05, 0x06, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x01,
      0x00, 0x47, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
    };
  }

  private byte[] createValidDocBytes() {
    // OLE2 compound document header
    byte[] docBytes = new byte[512];
    // OLE2 signature
    docBytes[0] = (byte) 0xD0;
    docBytes[1] = (byte) 0xCF;
    docBytes[2] = (byte) 0x11;
    docBytes[3] = (byte) 0xE0;
    docBytes[4] = (byte) 0xA1;
    docBytes[5] = (byte) 0xB1;
    docBytes[6] = (byte) 0x1A;
    docBytes[7] = (byte) 0xE1;
    return docBytes;
  }

  private byte[] createValidJpegBytes() {
    return new byte[] {
      (byte) 0xFF,
      (byte) 0xD8,
      (byte) 0xFF,
      (byte) 0xE0,
      0x00,
      0x10,
      0x4A,
      0x46,
      0x49,
      0x46,
      0x00,
      0x01,
      0x01,
      0x01,
      0x00,
      0x48,
      0x00,
      0x48,
      0x00,
      0x00,
      (byte) 0xFF,
      (byte) 0xD9
    };
  }

  private byte[] createValidPngBytes() {
    return new byte[] {
      (byte) 0x89,
      0x50,
      0x4E,
      0x47,
      0x0D,
      0x0A,
      0x1A,
      0x0A,
      0x00,
      0x00,
      0x00,
      0x0D,
      0x49,
      0x48,
      0x44,
      0x52,
      0x00,
      0x00,
      0x00,
      0x01,
      0x00,
      0x00,
      0x00,
      0x01,
      0x08,
      0x06,
      0x00,
      0x00,
      0x00,
      0x1F,
      0x15,
      (byte) 0xC4,
      (byte) 0x89,
      0x00,
      0x00,
      0x00,
      0x0D,
      0x49,
      0x44,
      0x41,
      0x54,
      0x78,
      (byte) 0xDA,
      0x63,
      0x00,
      0x01,
      0x00,
      0x00,
      0x05,
      0x00,
      0x01,
      0x0D,
      0x0A,
      0x2D,
      (byte) 0xB4,
      0x00,
      0x00,
      0x00,
      0x00,
      0x49,
      0x45,
      0x4E,
      0x44,
      (byte) 0xAE,
      0x42,
      0x60,
      (byte) 0x82
    };
  }

  private byte[] createExecutableBytes() {
    return new byte[] {
      0x4D,
      0x5A,
      (byte) 0x90,
      0x00,
      0x03,
      0x00,
      0x00,
      0x00,
      0x04,
      0x00,
      0x00,
      0x00,
      (byte) 0xFF,
      (byte) 0xFF,
      0x00,
      0x00,
      (byte) 0xB8,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x00,
      0x40,
      0x00,
      0x00,
      0x00
    };
  }

  private byte[] createGenericBinaryContent() {
    return new byte[] {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09};
  }

  private byte[] getValidContentForExtension(String extension) {
    switch (extension.toLowerCase()) {
      case "pdf":
        return createValidPdfBytes();
      case "docx":
        return createValidDocxBytes();
      case "doc":
        return createValidDocBytes();
      case "jpg":
      case "jpeg":
        return createValidJpegBytes();
      case "png":
        return createValidPngBytes();
      case "txt":
        return "This is a text file.".getBytes();
      case "csv":
        return "Name,Age\nJohn,30\nJane,25".getBytes();
      case "xls":
      case "xlsx":
        return createValidDocBytes(); // Simple binary content for Excel files
      default:
        return "Generic content".getBytes();
    }
  }

  private String getMimeTypeForExtension(String extension) {
    switch (extension.toLowerCase()) {
      case "pdf":
        return "application/pdf";
      case "docx":
        return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
      case "doc":
        return "application/msword";
      case "jpg":
      case "jpeg":
        return "image/jpeg";
      case "png":
        return "image/png";
      case "txt":
        return "text/plain";
      case "csv":
        return "text/csv";
      case "xls":
        return "application/vnd.ms-excel";
      case "xlsx":
        return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
      default:
        return "application/octet-stream";
    }
  }

  private static Stream<Arguments> provideTikaDetectedTypes() {
    return Stream.of(
        Arguments.of("test.doc", "application/msword", createStaticValidDocBytes()),
        Arguments.of(
            "test.docx",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            createStaticValidDocxBytes()),
        Arguments.of("test.pdf", "application/pdf", createStaticValidPdfBytes()));
  }

  private static byte[] createStaticValidDocBytes() {
    byte[] docBytes = new byte[512];
    // OLE2 signature
    docBytes[0] = (byte) 0xD0;
    docBytes[1] = (byte) 0xCF;
    docBytes[2] = (byte) 0x11;
    docBytes[3] = (byte) 0xE0;
    docBytes[4] = (byte) 0xA1;
    docBytes[5] = (byte) 0xB1;
    docBytes[6] = (byte) 0x1A;
    docBytes[7] = (byte) 0xE1;
    return docBytes;
  }

  private static byte[] createStaticValidDocxBytes() {
    return new byte[] {
      0x50, 0x4B, 0x03, 0x04, 0x14, 0x00, 0x00, 0x00, 0x08, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
      0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x19, 0x00, 0x00, 0x00,
      0x5B, 0x43, 0x6F, 0x6E, 0x74, 0x65, 0x6E, 0x74, 0x5F, 0x54, 0x79, 0x70, 0x65, 0x73, 0x5D,
      0x2E, 0x78, 0x6D, 0x6C, 0x50, 0x4B, 0x05, 0x06, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x01,
      0x00, 0x47, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
    };
  }

  private static byte[] createStaticValidPdfBytes() {
    return ("%PDF-1.4\n1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj\n"
            + "2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj\n"
            + "3 0 obj<</Type/Page/MediaBox[0 0 612 792]/Parent 2 0 R>>endobj\n"
            + "xref\n0 4\n0000000000 65535 f \n0000000010 00000 n \n"
            + "0000000053 00000 n \n0000000125 00000 n \n"
            + "trailer<</Size 4/Root 1 0 R>>\nstartxref\n182\n%%EOF")
        .getBytes();
  }
}
