package eu.bbmri_eric.negotiator.webhook.event;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Webhook data payload for a resource state transition in a negotiation.
 *
 * <p>The States and the Event are names carried as data: they are JSON strings on the wire, exactly
 * as they were when this record held enums.
 *
 * @param negotiationId identifier of the parent negotiation
 * @param resourceId identifier of the affected resource
 * @param fromState name of the previous resource state
 * @param toState name of the new resource state
 * @param event name of the resource lifecycle event that triggered the transition
 */
@WebhookEventDoc(
    summary = "Negotiation resource state changed",
    description =
        "Sent when the state of a resource within a negotiation changes through its lifecycle.")
@Schema(description = "Webhook data payload for a negotiation resource state transition.")
record NegotiationResourceStateUpdatedWebhookEvent(
    @Schema(
            description = "Identifier of the parent negotiation",
            example = "ab222d9c-4567-4c9a-8f0e-123456789abc")
        String negotiationId,
    @Schema(
            description = "Identifier of the affected resource",
            example = "dcba1234-5678-4c9a-8f0e-123456789abc")
        String resourceId,
    @Schema(
            description = "Name of the source resource state before transition",
            example = "SUBMITTED")
        String fromState,
    @Schema(
            description = "Name of the target resource state after transition",
            example = "RESOURCE_AVAILABLE")
        String toState,
    @Schema(
            description = "Name of the resource lifecycle event that triggered the transition",
            example = "MARK_AS_AVAILABLE")
        String event) {}
