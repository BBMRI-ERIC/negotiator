package eu.bbmri_eric.negotiator.webhook.event;

import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.NegotiationResourceEvent;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.NegotiationResourceState;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Webhook data payload for a resource state transition in a negotiation.
 *
 * @param negotiationId identifier of the parent negotiation
 * @param resourceId identifier of the affected resource
 * @param fromState previous resource state
 * @param toState new resource state
 * @param event resource-state-machine event that triggered the transition
 */
@WebhookEventDoc(
    summary = "Negotiation resource state changed",
    description =
        "Sent when the state of a resource within a negotiation changes through the state machine.")
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
    @Schema(description = "Source resource state before transition", example = "SUBMITTED")
        NegotiationResourceState fromState,
    @Schema(description = "Target resource state after transition", example = "RESOURCE_AVAILABLE")
        NegotiationResourceState toState,
    @Schema(
            description = "State-machine event that triggered the transition",
            example = "MARK_AS_AVAILABLE")
        NegotiationResourceEvent event) {}
