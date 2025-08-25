package eu.bbmri_eric.negotiator.negotiation.pdf;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;
import org.thymeleaf.context.Context;

@Component
@CommonsLog
public class PdfContextBuilder {
  private final ObjectMapper objectMapper;

  @Value("${negotiator.emailLogo}")
  private String logoURL;

  private static final DateTimeFormatter DTF =
      DateTimeFormatter.ofPattern("MMMM dd, yyyy - h:mm a");

  public PdfContextBuilder(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Transactional
  public Context createContext(Negotiation negotiation) throws JsonProcessingException {
    try {
      Map<String, Object> payload =
          this.objectMapper.readValue(negotiation.getPayload(), new TypeReference<>() {});
      Context context = new Context();
      context.setVariable("now", LocalDateTime.now().format(DTF));
      context.setVariable("logoUrl", logoURL);
      context.setVariable("authorName", negotiation.getCreatedBy().getName());
      context.setVariable("authorEmail", negotiation.getCreatedBy().getEmail());
      context.setVariable("negotiationId", negotiation.getId());
      context.setVariable("negotiationCreatedAt", negotiation.getCreationDate());
      context.setVariable("negotiationStatus", negotiation.getCurrentState());
      context.setVariable("negotiationPayload", processPayload(payload));
      return context;
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
    return new Context();
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

  private String escapeHtml(String input) {
    String escapedText = HtmlUtils.htmlEscape(input);

    return escapedText.replace("\n", "<br />");
  }
}
