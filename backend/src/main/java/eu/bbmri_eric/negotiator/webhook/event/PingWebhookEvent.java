package eu.bbmri_eric.negotiator.webhook.event;

/**
 * Webhook data payload for a ping delivery.
 *
 * @param webhookId identifier of the target webhook
 */
record PingWebhookEvent(Long webhookId) {}
