package eu.bbmri_eric.negotiator.attachment;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

class DocConverterTest {

  private final DocConverter converter = new DocConverter();

  @Test
  void convertToPdf_WithValidDoc_ReturnsPdfContainingTheDocumentText() throws Exception {
    byte[] pdf = converter.convertToPdf(loadTestFile("test-valid.doc"));

    assertTrue(new String(pdf, StandardCharsets.ISO_8859_1).startsWith("%PDF"));
    try (PDDocument document = Loader.loadPDF(pdf)) {
      assertTrue(document.getNumberOfPages() > 0);
      assertFalse(new PDFTextStripper().getText(document).isBlank());
    }
  }

  @Test
  void convertToPdf_WithoutInput_ThrowsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () -> converter.convertToPdf(null));
    assertThrows(IllegalArgumentException.class, () -> converter.convertToPdf(new byte[0]));
  }

  private byte[] loadTestFile(String filename) throws Exception {
    try (InputStream inputStream = getClass().getResourceAsStream("/test-documents/" + filename)) {
      if (inputStream == null) {
        throw new IllegalArgumentException("Test file not found: " + filename);
      }
      return inputStream.readAllBytes();
    }
  }
}
