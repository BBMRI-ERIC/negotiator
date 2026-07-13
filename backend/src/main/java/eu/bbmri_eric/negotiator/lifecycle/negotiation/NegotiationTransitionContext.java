package eu.bbmri_eric.negotiator.lifecycle.negotiation;

import eu.bbmri_eric.negotiator.lifecycle.NegotiatorTransitionContext;
import java.util.Set;

public record NegotiationTransitionContext(
    String negotiationId, Set<String> roles, String postBody, Long userId)
    implements NegotiatorTransitionContext {}
