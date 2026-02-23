package eu.bbmri_eric.negotiator.webhook.event;

/**
 * Webhook data payload for newly added resources in a negotiation.
 *
 * @param negotiationId identifier of the affected negotiation
 */
record NegotiationResourceUpdatedWebhookEvent(String negotiationId) {}
