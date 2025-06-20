package eu.bbmri_eric.negotiator.negotiation.pdf;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.pdf.BaseFont;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

@Service
public class NegotiationPdfServiceImpl implements NegotiationPdfService {

  private TemplateEngine templateEngine;
  private ObjectMapper objectMapper;

  @Value("${negotiator.pdfFont}")
  private String fontPath;

  @Value("${negotiator.emailLogo}")
  private String logoURL;

  public NegotiationPdfServiceImpl(TemplateEngine templateEngine, ObjectMapper objectMapper) {
    this.templateEngine = templateEngine;
    this.objectMapper = objectMapper;
  }

  private Map<String, Object> processPayload(Map<String, Object> payload) {
    payload.replaceAll(
        (key, value) -> {
          if (value instanceof String str) {
            return escapeHtml(str).replaceAll("(<br />)+$", "");
          } else if (value instanceof Map<?, ?> map) {
            if (map.isEmpty()) {
              value = "Empty";
              return value;
            }
            return processPayload((Map<String, Object>) map);

          } else if (value instanceof Iterable<?> iterable) {
            return StreamSupport.stream(iterable.spliterator(), false)
                .map(
                    item -> {
                      if (item instanceof String s) {
                        return escapeHtml(s);
                      }
                      return item;
                    })
                .collect(Collectors.toList());
          }
          return value;
        });
    return payload;
  }

  private String escapeHtml(String input) {
    if (input == null) {
      return null;
    }
    return input
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#x27;")
        .replace("\n", "<br />");
  }

  public byte[] generatePdf(Negotiation negotiation, String templateName) throws Exception {
    Context context = new Context();
    context.setVariable(
        "now", LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy - h:mm a")));
    Map<String, Object> payload =
        this.objectMapper.readValue(negotiation.getPayload(), new TypeReference<>() {});
    context.setVariable(
        "negotiationPdfData",
        Map.of(
            "author", negotiation.getCreatedBy(),
            "id", negotiation.getId(),
            "createdAt", negotiation.getCreationDate(),
            "status", negotiation.getCurrentState(),
            "payload", processPayload(payload)));
    context.setVariable("logoUrl", logoURL);
    try {
      String renderedHtml = templateEngine.process(templateName, context);
      renderedHtml = renderedHtml.replaceAll("(<br />)+$", "");
      try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
        ITextRenderer renderer = new ITextRenderer();
        URL fontUrl = getClass().getResource(fontPath);
        if (fontUrl == null) {
          throw new FileNotFoundException(
                  String.format("Font not found at path: %s ", fontPath));
        }
        renderer.getFontResolver().addFont(
                fontUrl.toString(), BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
        renderer.setDocumentFromString(renderedHtml);
        renderer.layout();
        renderer.createPDF(outputStream);
        return outputStream.toByteArray();
      }
    } catch (Exception e) {
      throw new Exception("Error processing template: " + e.getMessage(), e);
    }
  }
}
