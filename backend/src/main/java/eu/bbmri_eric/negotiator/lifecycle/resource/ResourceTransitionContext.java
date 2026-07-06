package eu.bbmri_eric.negotiator.lifecycle.resource;

import eu.bbmri_eric.negotiator.lifecycle.NegotiatorTransitionContext;
import java.util.Set;

public record ResourceTransitionContext(String negotiationId, Set<String> roles, String resourceId)
    implements NegotiatorTransitionContext {}
