package eu.bbmri_eric.negotiator.lifecycle;

import eu.bbmri_eric.negotiator.lifecycle.statemachine.TransitionContext;
import java.util.Set;

/**
 * Not sealed: Java requires a sealed interface's permitted subtypes to share its package when
 * compiled without a named module (JLS 8.1.5), but NegotiationTransitionContext/
 * ResourceTransitionContext live in their own lifecycle.negotiation/lifecycle.resource packages.
 */
public interface NegotiatorTransitionContext extends TransitionContext {

  String negotiationId();

  Set<String> roles();
}
