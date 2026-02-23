package eu.bbmri_eric.negotiator.webhook;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.negotiator.webhook.event.WebhookEventEnvelope;
import eu.bbmri_eric.negotiator.webhook.event.WebhookEventMapper;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.context.ApplicationEvent;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listens for specific application events and dispatches them to active webhooks. This listener
 * serializes mapped webhook envelopes and delivers them to all configured webhooks using the {@link
 * WebhookService}.
 */
@Component
@CommonsLog
class WebhookEventListener {
  private final WebhookService webhookService;
  private final ObjectMapper objectMapper;
  private final WebhookEventMapper webhookEventMapper;

  WebhookEventListener(
      WebhookService webhookService,
      ObjectMapper objectMapper,
      WebhookEventMapper webhookEventMapper) {
    this.webhookService = webhookService;
    this.objectMapper = objectMapper;
    this.webhookEventMapper = webhookEventMapper;
  }

  /**
   * Handles application events asynchronously and dispatches them to webhooks if supported.
   *
   * @param event the application event to process
   */
  @Async
  @TransactionalEventListener(fallbackExecution = true)
  void onWebhookEvent(ApplicationEvent event) {
    webhookEventMapper.map(event).ifPresent(this::dispatch);
  }

  private void dispatch(WebhookEventEnvelope<?> eventEnvelope) {
    String payload = serializePayload(eventEnvelope);
    if (payload == null) {
      return;
    }
    webhookService.deliverToActiveWebhooks(payload, eventEnvelope.eventType());
  }

  private String serializePayload(WebhookEventEnvelope<?> eventEnvelope) {
    try {
      return objectMapper.writeValueAsString(eventEnvelope);
    } catch (JsonProcessingException e) {
      log.error(
          "Failed to serialize webhook payload for event: %s".formatted(eventEnvelope.eventType()),
          e);
      return null;
    }
  }
}
