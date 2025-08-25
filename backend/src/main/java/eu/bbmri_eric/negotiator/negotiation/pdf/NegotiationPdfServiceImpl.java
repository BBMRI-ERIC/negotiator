package eu.bbmri_eric.negotiator.negotiation.pdf;

import com.lowagie.text.pdf.BaseFont;
import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.common.exceptions.PdfGenerationException;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import jakarta.transaction.Transactional;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.xhtmlrenderer.pdf.ITextRenderer;

@Service(value = "DefaultNegotiationPdfService")
@CommonsLog
public class NegotiationPdfServiceImpl implements NegotiationPdfService {
  private static final String DEFAULT_PDF_TEMPLATE_NAME = "PDF_NEGOTIATION_SUMMARY";
  private final NegotiationRepository negotiationRepository;
  private final PdfContextBuilder pdfContextBuilder;

  @Value("${negotiator.pdfFont}")
  private String fontPath;

  public NegotiationPdfServiceImpl(
      NegotiationRepository negotiationRepository,
      PdfContextBuilder pdfContextBuilder) {
    this.negotiationRepository = negotiationRepository;
    this.pdfContextBuilder = pdfContextBuilder;
  }

  @Override
  @Transactional
  public byte[] generatePdf(String negotiationId) throws PdfGenerationException {
    Negotiation negotiation = findEntityById(negotiationId);
    try {
      String renderedHtml = pdfContextBuilder.createPdfContent(negotiation, DEFAULT_PDF_TEMPLATE_NAME)
          .replaceAll("(<br />)+$", "");
      return renderPdf(renderedHtml);
    } catch (Exception e) {
      throw new RuntimeException("Error creating negotiation pdf: " + e.getMessage());
    }
  }

  private Negotiation findEntityById(String negotiationId) {
    return negotiationRepository
        .findDetailedById(negotiationId)
        .orElseThrow(() -> new EntityNotFoundException(negotiationId));
  }

  private byte[] renderPdf(String html) throws IOException {
    URL fontUrl = resolveFontUrl();
    if (fontUrl == null) {
      throw new FileNotFoundException("PDF font not found: " + fontPath);
    }

    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      ITextRenderer renderer = new ITextRenderer();
      renderer
          .getFontResolver()
          .addFont(fontUrl.toString(), BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
      renderer.setDocumentFromString(html);
      renderer.layout();
      renderer.createPDF(outputStream);

      return outputStream.toByteArray();
    }
  }

  /**
   * Resolves the font URL from the configured font path. Supports both classpath resources and file
   * system paths.
   *
   * @return URL to the font file, or null if not found
   * @throws IOException if there's an error accessing the font file
   */
  private URL resolveFontUrl() throws IOException {
    if (fontPath == null || fontPath.trim().isEmpty()) {
      throw new IllegalArgumentException("Font path is not configured");
    }

    // First, try to load as a classpath resource
    URL classpathUrl = getClass().getResource(fontPath);
    if (classpathUrl != null) {
      log.debug("Font loaded from classpath: " + fontPath);
      return classpathUrl;
    }

    // If not found on classpath, try as a file system path
    Path filePath = Paths.get(fontPath);
    File fontFile = filePath.toFile();

    if (fontFile.exists() && fontFile.isFile()) {
      try {
        URL fileUrl = fontFile.toURI().toURL();
        log.debug("Font loaded from file system: " + fontPath);
        return fileUrl;
      } catch (MalformedURLException e) {
        throw new IOException("Invalid font file path: " + fontPath, e);
      }
    }

    // Try relative to classpath root if path doesn't start with '/'
    if (!fontPath.startsWith("/")) {
      String classpathPath = "/" + fontPath;
      classpathUrl = getClass().getResource(classpathPath);
      if (classpathUrl != null) {
        log.debug("Font loaded from classpath with leading slash: " + classpathPath);
        return classpathUrl;
      }
    }

    log.error("Font not found at path: " + fontPath);
    return null;
  }
}
