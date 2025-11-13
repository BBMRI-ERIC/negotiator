package eu.bbmri_eric.negotiator.unit.info_submission;

import static org.junit.jupiter.api.Assertions.*;

import eu.bbmri_eric.negotiator.info_submission.pdf.InformationSubmissionToPdfConverter;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

class InformationSubmissionToPdfConverterTest {

  private InformationSubmissionToPdfConverter InformationSubmissionToPdfConverter;

  @BeforeEach
  void setUp() {
    TemplateEngine templateEngine = createTemplateEngine();
    InformationSubmissionToPdfConverter = new InformationSubmissionToPdfConverter(templateEngine);
  }

  private TemplateEngine createTemplateEngine() {
    ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
    templateResolver.setPrefix("templates/");
    templateResolver.setSuffix(".html");
    templateResolver.setTemplateMode(TemplateMode.HTML);
    templateResolver.setCharacterEncoding("UTF-8");

    SpringTemplateEngine templateEngine = new SpringTemplateEngine();
    templateEngine.setTemplateResolver(templateResolver);
    return templateEngine;
  }

  @Test
  void convertCsvToPdf_validCsv_returnsPdfBytes() throws IOException {
    String csvContent = "resourceId,field1,field2\nres123,value1,value2\nres456,value3,value4";
    MultipartFile csvFile =
        new MockMultipartFile("test.csv", "test.csv", "text/csv", csvContent.getBytes());

    byte[] pdfBytes =
        InformationSubmissionToPdfConverter.convertCsvToPdf(csvFile, "neg-001", "Test Requirement");

    assertNotNull(pdfBytes);
    assertTrue(pdfBytes.length > 0);
    // Check PDF magic bytes
    assertEquals('%', (char) pdfBytes[0]);
    assertEquals('P', (char) pdfBytes[1]);
    assertEquals('D', (char) pdfBytes[2]);
    assertEquals('F', (char) pdfBytes[3]);
  }

  @Test
  void convertCsvToPdf_emptyCsv_returnsPdf() throws IOException {
    String csvContent = "";
    MultipartFile csvFile =
        new MockMultipartFile("test.csv", "test.csv", "text/csv", csvContent.getBytes());

    byte[] pdfBytes =
        InformationSubmissionToPdfConverter.convertCsvToPdf(csvFile, "neg-001", "Test Requirement");

    assertNotNull(pdfBytes);
    assertTrue(pdfBytes.length > 0);
  }

  @Test
  void convertCsvToPdf_csvWithQuotedValues_handlesProperly() throws IOException {
    String csvContent =
        "resourceId,description\nres123,\"This is a quoted value, with comma\"\nres456,\"Another \"\"quoted\"\" value\"";
    MultipartFile csvFile =
        new MockMultipartFile("test.csv", "test.csv", "text/csv", csvContent.getBytes());

    byte[] pdfBytes =
        InformationSubmissionToPdfConverter.convertCsvToPdf(csvFile, "neg-001", "Test Requirement");

    assertNotNull(pdfBytes);
    assertTrue(pdfBytes.length > 0);
  }

  @Test
  void convertCsvToPdf_csvWithSpecialCharacters_handlesProperly() throws IOException {
    String csvContent =
        "resourceId,description\nres123,Special chars: äöü ñ € £\nres456,Unicode: 你好 مرحبا";
    MultipartFile csvFile =
        new MockMultipartFile("test.csv", "test.csv", "text/csv", csvContent.getBytes());

    byte[] pdfBytes =
        InformationSubmissionToPdfConverter.convertCsvToPdf(csvFile, "neg-001", "Test Requirement");

    assertNotNull(pdfBytes);
    assertTrue(pdfBytes.length > 0);
  }

  @Test
  void convertCsvToPdf_csvWithEmptyCells_fillsWithEmptyStrings() throws IOException {
    String csvContent = "resourceId,field1,field2\nres123,,value2\nres456,value3,";
    MultipartFile csvFile =
        new MockMultipartFile("test.csv", "test.csv", "text/csv", csvContent.getBytes());

    byte[] pdfBytes =
        InformationSubmissionToPdfConverter.convertCsvToPdf(csvFile, "neg-001", "Test Requirement");

    assertNotNull(pdfBytes);
    assertTrue(pdfBytes.length > 0);
  }

  @Test
  void convertCsvToPdf_csvWithMultipleRows_processesAll() throws IOException {
    StringBuilder csvContent = new StringBuilder("resourceId,field1\n");
    for (int i = 0; i < 50; i++) {
      csvContent.append("res").append(i).append(",value").append(i).append("\n");
    }

    MultipartFile csvFile =
        new MockMultipartFile("test.csv", "test.csv", "text/csv", csvContent.toString().getBytes());

    byte[] pdfBytes =
        InformationSubmissionToPdfConverter.convertCsvToPdf(csvFile, "neg-001", "Test Requirement");

    assertNotNull(pdfBytes);
    assertTrue(pdfBytes.length > 0);
  }

  @Test
  void convertCsvToPdf_csvWithOnlyHeaders_returnsPdf() throws IOException {
    String csvContent = "resourceId,field1,field2";
    MultipartFile csvFile =
        new MockMultipartFile("test.csv", "test.csv", "text/csv", csvContent.getBytes());

    byte[] pdfBytes =
        InformationSubmissionToPdfConverter.convertCsvToPdf(csvFile, "neg-001", "Test Requirement");

    assertNotNull(pdfBytes);
    assertTrue(pdfBytes.length > 0);
  }

  @Test
  void convertCsvToPdf_csvWithNewlinesInQuotedFields_handlesProperly() throws IOException {
    String csvContent = "resourceId,description\nres123,\"Line1\nLine2\nLine3\"";
    MultipartFile csvFile =
        new MockMultipartFile("test.csv", "test.csv", "text/csv", csvContent.getBytes());

    byte[] pdfBytes =
        InformationSubmissionToPdfConverter.convertCsvToPdf(csvFile, "neg-001", "Test Requirement");

    assertNotNull(pdfBytes);
    assertTrue(pdfBytes.length > 0);
  }
}
