package eu.bbmri_eric.negotiator.webhook.event;

/**
 * Webhook data payload for a newly created negotiation post.
 *
 * @param postId identifier of the created post
 * @param negotiationId identifier of the negotiation containing the post
 * @param userId identifier of the user that created the post
 * @param organizationId optional identifier of the organization context
 */
record NegotiationPostAddedWebhookEvent(
    String postId, String negotiationId, Long userId, Long organizationId) {}
