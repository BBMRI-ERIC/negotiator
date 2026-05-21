package eu.bbmri_eric.negotiator.webhook.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.ApplicationEvent;

/** Strategy for mapping a specific {@link ApplicationEvent} subtype to webhook payloads. */
public interface WebhookMappingStrategy<T extends ApplicationEvent> {

  /**
   * @return application event type supported by this strategy
   */
  Class<T> getSupportedEventType();

  /**
   * Maps the event to an outbound webhook payload envelope.
   *
   * @param event source application event
   * @param objectMapper mapper configured for webhook conversion
   * @return mapped payload envelope or empty when nothing should be published
   */
  Optional<WebhookPayloadEnvelope<?>> map(T event, ObjectMapper objectMapper);

  /**
   * Exposes payload schema metadata for webhook OpenAPI documentation.
   *
   * @return webhook event to payload class mapping documented by this strategy
   */
  Map<WebhookEventType, Class<?>> documentedPayloadTypes();
}
