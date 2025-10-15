package eu.bbmri_eric.negotiator.negotiation.pdf;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.pdf.BaseFont;
import eu.bbmri_eric.negotiator.attachment.AttachmentConversionService;
import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.common.exceptions.PdfGenerationException;
import eu.bbmri_eric.negotiator.governance.resource.Resource;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import jakarta.transaction.Transactional;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import lombok.extern.apachecommons.CommonsLog;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.HtmlUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

@Service(value = "DefaultNegotiationPdfService")
@CommonsLog
@Transactional
public class NegotiationPdfServiceImpl implements NegotiationPdfService {
  private static final DateTimeFormatter DTF =
      DateTimeFormatter.ofPattern("MMMM dd, yyyy - h:mm a");
  private static final String DEFAULT_PDF_TEMPLATE_NAME = "PDF_NEGOTIATION_SUMMARY";
  private final NegotiationRepository negotiationRepository;
  private final AttachmentConversionService conversionService;
  private final TemplateEngine templateEngine;
  private final ObjectMapper objectMapper;

  @Value("${negotiator.pdfFont}")
  private String fontPath;

  @Value("${negotiator.emailLogo}")
  private String logoURL;

  public NegotiationPdfServiceImpl(
      NegotiationRepository negotiationRepository,
      TemplateEngine templateEngine,
      ObjectMapper objectMapper,
      AttachmentConversionService conversionService) {
    this.negotiationRepository = negotiationRepository;
    this.templateEngine = templateEngine;
    this.objectMapper = objectMapper;
    this.conversionService = conversionService;
  }

  public byte[] generatePdf(String negotiationId, boolean includeAttachments)
      throws PdfGenerationException {
    Negotiation negotiation = findEntityById(negotiationId);
    Context context = createContext(negotiation, includeAttachments);
    String pdfContentAsHTML =
        templateEngine.process(DEFAULT_PDF_TEMPLATE_NAME, context).replaceAll("(<br />)+$", "");
    byte[] pdfBytes = renderPdf(pdfContentAsHTML);
    if (includeAttachments) {
      return pdfWithAttachments(negotiationId, pdfBytes);
    } else {
      return pdfBytes;
    }
  }

  private byte @NotNull [] pdfWithAttachments(String negotiationId, byte[] pdfBytes) {
    List<byte[]> pdfsToMerge =
        Stream.concat(
                Stream.of(pdfBytes),
                conversionService.listByNegotiationIdToPdf(negotiationId).stream())
            .toList();

    try {
      return PdfMerger.mergePdfs(pdfsToMerge);
    } catch (IOException e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error merging PDFs", e);
    }
  }

  private Negotiation findEntityById(String negotiationId) {
    return negotiationRepository
        .findDetailedById(negotiationId)
        .orElseThrow(() -> new EntityNotFoundException(negotiationId));
  }

  private String escapeHtml(String input) {
    String escapedText = HtmlUtils.htmlEscape(input);
    return escapedText.replace("\n", "<br />");
  }

  private Map<String, Object> processPayload(Map<String, Object> payload) {
    Map<String, Object> processedPayload = new LinkedHashMap<>();
    try {
      payload.forEach(
          (key, value) -> {
            if (value instanceof String str) {
              processedPayload.put(
                  key.replaceAll("-", " ").toUpperCase(),
                  escapeHtml(str).replaceAll("(<br />)+$", ""));
            } else if (value instanceof Map<?, ?> map) {
              if (map.isEmpty()) {
                processedPayload.put(key.replaceAll("-", " ").toUpperCase(), "Empty");
              } else {
                processedPayload.put(
                    key.replaceAll("-", " ").toUpperCase(),
                    processPayload((Map<String, Object>) map));
              }
            } else if (value instanceof Iterable<?> iterable) {
              processedPayload.put(
                  key.replaceAll("-", " ").toUpperCase(),
                  StreamSupport.stream(iterable.spliterator(), false)
                      .map(item -> (item instanceof String s) ? escapeHtml(s) : item)
                      .collect(Collectors.toList()));
            }
          });
    } catch (Exception e) {
      log.error("Error processing negotiation payload: " + e.getMessage());
    }
    return processedPayload;
  }

  private Map<String, Set<String>> getResourcesByOrganization(Set<Resource> resources) {
    Map<String, Set<String>> resourcesByOrganization = new LinkedHashMap<>();
    for (var resource : resources) {
      var orgName = resource.getOrganization().getName();
      resourcesByOrganization
          .computeIfAbsent(orgName, k -> new HashSet<>())
          .add(resource.getName());
    }
    return resourcesByOrganization;
  }

  private Context createContext(Negotiation negotiation, boolean includeAttachments) {
    Map<String, Object> payload = new LinkedHashMap<>();
    try {
      payload = this.objectMapper.readValue(negotiation.getPayload(), new TypeReference<>() {});
    } catch (JsonProcessingException e) {
      log.error("Error processing negotiation payload: " + e.getMessage());
    }
    Context context = new Context();
    try {
      context.setVariable("now", LocalDateTime.now().format(DTF));
      context.setVariable("logoUrl", logoURL);
      context.setVariable("authorName", negotiation.getCreatedBy().getName());
      context.setVariable("authorEmail", negotiation.getCreatedBy().getEmail());
      context.setVariable("authorInstitution", negotiation.getCreatedBy().getOrganization());
      context.setVariable("negotiationId", negotiation.getId());
      context.setVariable("negotiationTitle", negotiation.getTitle());
      context.setVariable("negotiationCreatedAt", negotiation.getCreationDate());
      context.setVariable("negotiationStatus", negotiation.getCurrentState());
      context.setVariable("negotiationPayload", processPayload(payload));
      context.setVariable(
          "resourcesByOrganization", getResourcesByOrganization(negotiation.getResources()));
      context.setVariable("includeAttachments", includeAttachments);
    } catch (Exception e) {
      log.error("Error creating thymeleaf Context for a PDF summary " + e.getMessage());
    }
    return context;
  }

  private byte[] renderPdf(String html) {
    URL fontUrl = resolveFontUrl();
    if (fontUrl == null) {
      log.error("Could not resolve fontUrl for the PDF content");
      throw new PdfGenerationException("Error generating the PDF summary");
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
    } catch (Exception e) {
      log.error("Error rendering PDF: " + e.getMessage());
    }
    throw new PdfGenerationException("Error generating the PDF summary");
  }

  /**
   * Resolves the font URL from the configured font path. Supports both classpath resources and file
   * system paths.
   *
   * @return URL to the font file, or null if not found
   */
  private URL resolveFontUrl() {
    if (fontPath == null || fontPath.trim().isEmpty()) {
      throw new IllegalArgumentException("Font path is not configured");
    }
    URL classpathUrl = getClass().getResource(fontPath);
    if (classpathUrl != null) {
      log.debug("Font loaded from classpath: " + fontPath);
      return classpathUrl;
    }
    Path filePath = Paths.get(fontPath);
    File fontFile = filePath.toFile();

    if (fontFile.exists() && fontFile.isFile()) {
      try {
        URL fileUrl = fontFile.toURI().toURL();
        log.debug("Font loaded from file system: " + fontPath);
        return fileUrl;
      } catch (MalformedURLException e) {
        log.error("Font file could not be loaded: " + fontPath);
        return null;
      }
    }
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
