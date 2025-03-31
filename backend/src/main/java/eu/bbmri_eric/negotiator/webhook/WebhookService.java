package eu.bbmri_eric.negotiator.webhook;

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
   * Creates a new delivery for a given webhook.
   *
   * @param dto the DTO containing the webhook ID and JSON content for the delivery
   * @return a DTO representing the newly created delivery
   */
  DeliveryDTO deliver(DeliveryCreateDTO dto, Long webhookId);
}
