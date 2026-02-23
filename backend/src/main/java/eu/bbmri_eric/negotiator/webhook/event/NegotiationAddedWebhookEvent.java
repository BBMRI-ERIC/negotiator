package eu.bbmri_eric.negotiator.webhook.event;

/**
 * Webhook data payload for a newly created negotiation.
 *
 * @param negotiationId identifier of the created negotiation
 */
record NegotiationAddedWebhookEvent(String negotiationId) {}
