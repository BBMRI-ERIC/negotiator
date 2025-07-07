package eu.bbmri_eric.negotiator.negotiation.pdf;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.pdf.BaseFont;
import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.common.exceptions.PdfGenerationException;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import jakarta.transaction.Transactional;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

@Service(value = "DefaultNegotiationPdfService")
@CommonsLog
@Transactional
public class NegotiationPdfServiceImpl implements NegotiationPdfService {
  private NegotiationRepository negotiationRepository;

  private TemplateEngine templateEngine;
  private ObjectMapper objectMapper;

  @Value("${negotiator.pdfFont}")
  private String fontPath;

  @Value("${negotiator.emailLogo}")
  private String logoURL;

  private static final DateTimeFormatter DTF =
      DateTimeFormatter.ofPattern("MMMM dd, yyyy - h:mm a");

  private static final String DEFAULT_PDF_TEMPLATE_NAME = "pdf-negotiation-summary";

  public NegotiationPdfServiceImpl(
      NegotiationRepository negotiationRepository,
      TemplateEngine templateEngine,
      ObjectMapper objectMapper) {
    this.negotiationRepository = negotiationRepository;
    this.templateEngine = templateEngine;
    this.objectMapper = objectMapper;
  }

  public byte[] generatePdf(String negotiationId, String templateName)
      throws PdfGenerationException {
    Negotiation negotiation = findEntityById(negotiationId);

    if (templateName == null) {
      templateName = DEFAULT_PDF_TEMPLATE_NAME;
    }

    try {
      Context context = createContext(negotiation);
      String renderedHtml =
          templateEngine.process(templateName, context).replaceAll("(<br />)+$", "");

      return renderPdf(renderedHtml);
    } catch (Exception e) {
      throw new PdfGenerationException("Error creating negotiation pdf: " + e.getMessage());
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
    payload.replaceAll(
        (key, value) -> {
          if (value instanceof String str) {
            return escapeHtml(str).replaceAll("(<br />)+$", "");
          } else if (value instanceof Map<?, ?> map) {
            if (map.isEmpty()) {
              return "Empty";
            }
            return processPayload((Map<String, Object>) map);
          } else if (value instanceof Iterable<?> iterable) {
            return StreamSupport.stream(iterable.spliterator(), false)
                .map(item -> (item instanceof String s) ? escapeHtml(s) : item)
                .collect(Collectors.toList());
          }
          return value;
        });
    return payload;
  }

  private Context createContext(Negotiation negotiation) throws JsonProcessingException {
    Map<String, Object> payload =
        this.objectMapper.readValue(negotiation.getPayload(), new TypeReference<>() {});

    Context context = new Context();
    context.setVariable("now", LocalDateTime.now().format(DTF));
    context.setVariable("logoUrl", logoURL);
    context.setVariable(
        "negotiationPdfData",
        Map.of(
            "author", negotiation.getCreatedBy(),
            "id", negotiation.getId(),
            "createdAt", negotiation.getCreationDate(),
            "status", negotiation.getCurrentState(),
            "payload", processPayload(payload)));

    return context;
  }

  private byte[] renderPdf(String html) throws IOException {
    URL fontUrl = getClass().getResource(fontPath);
    if (fontUrl == null) {
      throw new FileNotFoundException("PDF font not found on classpath: " + fontPath);
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
}
