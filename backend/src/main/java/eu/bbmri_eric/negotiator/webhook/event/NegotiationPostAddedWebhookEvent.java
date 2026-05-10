package eu.bbmri_eric.negotiator.webhook.event;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Webhook data payload for a newly created negotiation post.
 *
 * @param postId identifier of the created post
 * @param negotiationId identifier of the negotiation containing the post
 * @param userId identifier of the user that created the post
 * @param organizationId optional identifier of the organization context
 */
@WebhookEventDoc(
    summary = "Negotiation post added",
    description = "Sent when a new post is added to a negotiation discussion.")
@Schema(description = "Webhook data payload for a newly created negotiation post.")
record NegotiationPostAddedWebhookEvent(
    @Schema(description = "Identifier of the created post", example = "post-1") String postId,
    @Schema(
            description = "Identifier of the parent negotiation",
            example = "ab222d9c-4567-4c9a-8f0e-123456789abc")
        String negotiationId,
    @Schema(description = "Identifier of the user that created the post", example = "1000")
        Long userId,
    @Schema(
            description = "Optional identifier of the organization context",
            nullable = true,
            example = "2000")
        Long organizationId) {}
