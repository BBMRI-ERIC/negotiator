package eu.bbmri_eric.negotiator.webhook;

import eu.bbmri_eric.negotiator.webhook.event.WebhookEventType;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Submits a single webhook delivery as an independent async task on the dedicated virtual-thread
 * executor. One task is created per active webhook endpoint so that a slow or unreachable endpoint
 * does not block delivery to others.
 */
@Component
@RequiredArgsConstructor
class WebhookDeliveryDispatcher {

  private final WebhookService webhookService;

  /**
   * Schedules the delivery of a webhook event to the specified endpoint asynchronously.
   *
   * @param webhookId the id of the target webhook
   * @param payload the JSON payload to deliver
   * @param eventType the event type header for the delivery
   * @param occurredAt the event timestamp to include as a header
   */
  @Async("webhookDeliveryExecutor")
  void scheduleDelivery(
      Long webhookId, String payload, WebhookEventType eventType, Instant occurredAt) {
    webhookService.deliver(payload, eventType, webhookId, occurredAt);
  }
}
