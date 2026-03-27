package eu.bbmri_eric.negotiator.webhook;

import eu.bbmri_eric.negotiator.webhook.event.WebhookEventType;
import java.time.Instant;
import java.util.List;

public interface WebhookService {
  /**
   * Creates a new webhook using the provided data.
   *
   * @param dto the webhook creation data
   * @return the created webhook including its generated id
   */
  WebhookResponseDTO createWebhook(WebhookCreateDTO dto);

  /**
   * Fetches a single webhook by its id.
   *
   * @param id the identifier of the webhook
   * @return the webhook data, including its id
   */
  WebhookResponseDTO getWebhookById(Long id);

  /**
   * Fetches all available webhooks.
   *
   * @return a list of webhook data objects
   */
  List<WebhookResponseDTO> getAllWebhooks();

  /**
   * Updates an existing webhook with the provided data.
   *
   * @param id the identifier of the webhook to update
   * @param dto the new webhook data
   * @return the updated webhook data, including its id
   */
  WebhookResponseDTO updateWebhook(Long id, WebhookCreateDTO dto);

  /**
   * Deletes a webhook by its id.
   *
   * @param id the identifier of the webhook to delete
   */
  void deleteWebhook(Long id);

  /**
   * Creates a new delivery for a given webhook, using the current time as the occurred-at
   * timestamp.
   *
   * @param jsonPayload the JSON content for the delivery
   * @param eventType the event type header for the delivery
   * @param webhookId the id of the target webhook
   * @return a DTO representing the newly created delivery
   */
  DeliveryDTO deliver(String jsonPayload, WebhookEventType eventType, Long webhookId);

  /**
   * Creates a manual redelivery for a previously recorded delivery.
   *
   * @param webhookId the owning webhook id
   * @param deliveryId the source delivery id to redeliver
   * @return a DTO representing the newly created delivery attempt
   */
  DeliveryDTO redeliver(Long webhookId, String deliveryId);

  /**
   * Creates a new delivery for a given webhook with an explicit occurred-at timestamp.
   *
   * @param jsonPayload the JSON content for the delivery
   * @param eventType the event type header for the delivery
   * @param webhookId the id of the target webhook
   * @param occurredAt the event timestamp to include as a header
   * @return a DTO representing the newly created delivery
   */
  DeliveryDTO deliver(
      String jsonPayload, WebhookEventType eventType, Long webhookId, Instant occurredAt);

  /**
   * Returns the ids of all currently active webhooks.
   *
   * @return a list of active webhook ids
   */
  List<Long> getActiveWebhookIds();
}
