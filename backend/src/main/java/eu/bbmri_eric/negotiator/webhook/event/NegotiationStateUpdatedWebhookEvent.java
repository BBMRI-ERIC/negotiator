package eu.bbmri_eric.negotiator.webhook.event;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Webhook data payload for a negotiation state transition.
 *
 * <p>The States and the Event are names carried as data: they are JSON strings on the wire, exactly
 * as they were when this record held enums.
 *
 * @param negotiationId identifier of the affected negotiation
 * @param fromState name of the source negotiation state before transition
 * @param toState name of the target negotiation state after transition
 * @param event name of the lifecycle event that triggered the transition
 * @param postId optional identifier of the post submitted along with the state change
 */
@WebhookEventDoc(
    summary = "Negotiation state changed",
    description = "Sent when the negotiation lifecycle state transitions to a new state.")
@Schema(description = "Webhook data payload for a negotiation state transition.")
record NegotiationStateUpdatedWebhookEvent(
    @Schema(
            description = "Identifier of the affected negotiation",
            example = "550e8400-e29b-41d4-a716-446655440000")
        String negotiationId,
    @Schema(description = "Name of the source state before transition", example = "DRAFT")
        String fromState,
    @Schema(description = "Name of the target state after transition", example = "SUBMITTED")
        String toState,
    @Schema(
            description = "Name of the lifecycle event that triggered the transition",
            example = "SUBMIT")
        String event,
    @Schema(
            description = "Optional identifier of the post submitted along with the state change",
            nullable = true,
            example = "bb50ca76-daa0-4a1e-9d74-c0ae90922554")
        String postId) {}
