package eu.bbmri_eric.negotiator.lifecycle.evaluation;

import eu.bbmri_eric.negotiator.lifecycle.WellKnownNegotiationStates;
import eu.bbmri_eric.negotiator.lifecycle.graph.EvaluationContext;
import eu.bbmri_eric.negotiator.lifecycle.graph.GuardVerdict;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * The parent Negotiation must be in progress before any of its Resource Lifecycles will move.
 *
 * <p>This reproduces the check in {@code ResourceLifecycleServiceImpl}, which offers nothing unless
 * the parent is {@code IN_PROGRESS}. It does not port {@code NegotiationIsApprovedGuard}, which no
 * Transition ever used.
 *
 * <p>It reads the parent's State from the evaluation context, so it needs no port and does no I/O.
 *
 * <p>One Wiring row per Resource-scope Definition Version applies it to every Transition, so a
 * Transition added later cannot miss it. It stays a wired Guard rather than part of the evaluator
 * because a Network may want Resource work to begin before approval.
 */
@Component
class NegotiationApprovedGuard implements Guard<NoParams> {

  static final String TYPE_KEY = "NEGOTIATION_APPROVED";

  @Override
  public String typeKey() {
    return TYPE_KEY;
  }

  @Override
  public Class<NoParams> paramsType() {
    return NoParams.class;
  }

  @Override
  public GuardVerdict check(EvaluationContext context, NoParams params) {
    String parentState = context.parentNegotiationState();
    if (WellKnownNegotiationStates.IN_PROGRESS.equals(parentState)) {
      return GuardVerdict.pass();
    }
    return GuardVerdict.fail(
        "PARENT_NEGOTIATION_NOT_IN_PROGRESS",
        Map.of(
            "parentNegotiationState",
            parentState == null ? "none" : parentState,
            "requiredParentState",
            WellKnownNegotiationStates.IN_PROGRESS));
  }
}
