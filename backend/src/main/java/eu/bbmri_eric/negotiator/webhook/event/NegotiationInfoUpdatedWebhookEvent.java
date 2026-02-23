package eu.bbmri_eric.negotiator.webhook.event;

/**
 * Webhook data payload for an updated information submission in a negotiation.
 *
 * @param negotiationId identifier of the affected negotiation
 */
record NegotiationInfoUpdatedWebhookEvent(String negotiationId) {}
