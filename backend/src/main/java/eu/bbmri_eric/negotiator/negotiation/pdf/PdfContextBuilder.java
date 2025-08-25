package eu.bbmri_eric.negotiator.negotiation.pdf;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.template.TemplateService;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

@Component
@CommonsLog
public class PdfContextBuilder {
  private final ObjectMapper objectMapper;
  private final TemplateService templateService;

  @Value("${negotiator.emailLogo}")
  private String logoURL;

  private static final DateTimeFormatter DTF =
      DateTimeFormatter.ofPattern("MMMM dd, yyyy - h:mm a");

  public PdfContextBuilder(ObjectMapper objectMapper, TemplateService templateService) {
    this.objectMapper = objectMapper;
    this.templateService = templateService;
  }

  @Transactional
  public String createPdfContent(Negotiation negotiation, String templateName) {
    try {
      Map<String, Object> payload =
          this.objectMapper.readValue(negotiation.getPayload(), new TypeReference<>() {});

      Map<String, Object> variables = new HashMap<>();
      variables.put("now", LocalDateTime.now().format(DTF));
      variables.put("logoUrl", logoURL);
      variables.put("authorName", negotiation.getCreatedBy().getName());
      variables.put("authorEmail", negotiation.getCreatedBy().getEmail());
      variables.put("negotiationId", negotiation.getId());
      variables.put("negotiationCreatedAt", negotiation.getCreationDate());
      variables.put("negotiationStatus", negotiation.getCurrentState());
      variables.put("negotiationPayload", processPayload(payload));

      return templateService.processTemplate(variables, templateName);
    } catch (Exception e) {
      log.error("Failed to create PDF content for negotiation " + negotiation.getId(), e);
      return "";
    }
  }

  @SuppressWarnings("unchecked")
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

  private String escapeHtml(String input) {
    String escapedText = HtmlUtils.htmlEscape(input);

    return escapedText.replace("\n", "<br />");
  }
}
