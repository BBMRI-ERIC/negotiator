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
 */
@WebhookEventDoc(
    summary = "Negotiation state changed",
    description = "Sent when the negotiation lifecycle state transitions to a new state.")
@Schema(description = "Webhook data payload for a negotiation state transition.")
record NegotiationStateUpdatedWebhookEvent(
    @Schema(description = "Identifier of the affected negotiation", example = "negotiation-2")
        String negotiationId,
    @Schema(description = "Name of the source state before transition", example = "DRAFT")
        String fromState,
    @Schema(description = "Name of the target state after transition", example = "SUBMITTED")
        String toState,
    @Schema(
            description = "Name of the lifecycle event that triggered the transition",
            example = "SUBMIT")
        String event) {}
