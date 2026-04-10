package eu.bbmri_eric.negotiator.webhook;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.negotiator.webhook.event.WebhookEventMapper;
import eu.bbmri_eric.negotiator.webhook.event.WebhookPayloadEnvelope;
import java.util.List;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.context.ApplicationEvent;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listens for supported application events and schedules per-endpoint webhook deliveries. After a
 * transaction commits, this listener maps the event, serializes the payload, and submits an
 * independent async task for each active webhook via {@link WebhookDeliveryDispatcher}, enabling
 * concurrent delivery so a slow or unreachable endpoint does not block others.
 */
@Component
@CommonsLog
class WebhookEventListener {
  private final WebhookService webhookService;
  private final ObjectMapper objectMapper;
  private final WebhookEventMapper webhookEventMapper;
  private final WebhookDeliveryDispatcher webhookDeliveryDispatcher;

  WebhookEventListener(
      WebhookService webhookService,
      ObjectMapper objectMapper,
      WebhookEventMapper webhookEventMapper,
      WebhookDeliveryDispatcher webhookDeliveryDispatcher) {
    this.webhookService = webhookService;
    this.objectMapper = objectMapper;
    this.webhookEventMapper = webhookEventMapper;
    this.webhookDeliveryDispatcher = webhookDeliveryDispatcher;
  }

  /**
   * Handles application events asynchronously and dispatches them to webhooks if supported. Runs
   * after the transaction commits to ensure all data is visible in the database.
   *
   * @param event the application event to process
   */
  @Async
  @TransactionalEventListener(fallbackExecution = true)
  void onWebhookEvent(ApplicationEvent event) {
    webhookEventMapper.map(event).ifPresent(this::dispatch);
  }

  private void dispatch(WebhookPayloadEnvelope<?> payloadEnvelope) {
    String payload = serializePayload(payloadEnvelope);
    if (payload == null) {
      return;
    }
    List<Long> webhookIds = webhookService.getActiveWebhookIds();
    for (Long webhookId : webhookIds) {
      webhookDeliveryDispatcher.scheduleDelivery(
          webhookId, payload, payloadEnvelope.eventType(), payloadEnvelope.timestamp());
    }
  }

  private String serializePayload(Object payloadObject) {
    try {
      return objectMapper.writeValueAsString(payloadObject);
    } catch (JsonProcessingException e) {
      log.error("Failed to serialize webhook payload", e);
      return null;
    }
  }
}
