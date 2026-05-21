package eu.bbmri_eric.negotiator.webhook.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.ApplicationEvent;

/** Default mapping strategy for events that map directly to a fixed webhook event type and DTO. */
final class DefaultWebhookMappingStrategy<S extends ApplicationEvent, T>
    implements WebhookMappingStrategy<S> {

  private final Class<S> supportedEventType;
  private final Class<T> payloadType;
  private final WebhookEventType webhookEventType;

  private DefaultWebhookMappingStrategy(
      Class<S> supportedEventType, Class<T> payloadType, WebhookEventType webhookEventType) {
    this.supportedEventType = supportedEventType;
    this.payloadType = payloadType;
    this.webhookEventType = webhookEventType;
  }

  static <S extends ApplicationEvent, T> WebhookMappingStrategy<S> of(
      Class<S> supportedEventType, Class<T> payloadType, WebhookEventType webhookEventType) {
    return new DefaultWebhookMappingStrategy<>(supportedEventType, payloadType, webhookEventType);
  }

  @Override
  public Class<S> getSupportedEventType() {
    return supportedEventType;
  }

  @Override
  public Optional<WebhookPayloadEnvelope<?>> map(S event, ObjectMapper objectMapper) {
    T data = objectMapper.convertValue(event, payloadType);
    return Optional.of(
        new WebhookPayloadEnvelope<>(
            webhookEventType, Instant.ofEpochMilli(event.getTimestamp()), data));
  }

  @Override
  public Map<WebhookEventType, Class<?>> documentedPayloadTypes() {
    return Map.of(webhookEventType, payloadType);
  }
}
