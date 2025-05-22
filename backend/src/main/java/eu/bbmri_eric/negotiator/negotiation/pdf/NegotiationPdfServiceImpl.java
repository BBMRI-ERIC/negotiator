package eu.bbmri_eric.negotiator.negotiation.pdf;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

@Service
public class NegotiationPdfServiceImpl implements NegotiationPdfService {

  private TemplateEngine templateEngine;
  private ObjectMapper objectMapper;

  @Value("${negotiator.emailLogo}")
  private String logoURL;

  public NegotiationPdfServiceImpl(TemplateEngine templateEngine, ObjectMapper objectMapper) {
    this.templateEngine = templateEngine;
    this.objectMapper = objectMapper;
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
            "payload", payload));
    context.setVariable("logoUrl", logoURL);
    try {
      String renderedHtml = templateEngine.process(templateName, context);
      try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
        ITextRenderer renderer = new ITextRenderer();
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
