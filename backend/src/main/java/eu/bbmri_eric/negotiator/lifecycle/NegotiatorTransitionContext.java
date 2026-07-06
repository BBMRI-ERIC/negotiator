package eu.bbmri_eric.negotiator.lifecycle;

import eu.bbmri_eric.negotiator.lifecycle.statemachine.TransitionContext;
import java.util.Set;

/**
 * Common shape for negotiation/resource transition contexts. Not {@code sealed}: Java requires a
 * sealed type's permitted subtypes to live in the same package as the sealed type in an unnamed
 * module, which would force {@code NegotiationTransitionContext}/{@code ResourceTransitionContext}
 * out of their per-machine packages. {@link NegotiationTransitionContext} and {@link
 * ResourceTransitionContext} are its only two implementations.
 */
public interface NegotiatorTransitionContext extends TransitionContext {
  String negotiationId();

  Set<String> roles();
}
