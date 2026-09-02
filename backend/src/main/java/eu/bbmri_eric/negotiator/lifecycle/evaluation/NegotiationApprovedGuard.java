package eu.bbmri_eric.negotiator.lifecycle.evaluation;

import eu.bbmri_eric.negotiator.lifecycle.WellKnownNegotiationStates;
import eu.bbmri_eric.negotiator.lifecycle.graph.EvaluationContext;
import eu.bbmri_eric.negotiator.lifecycle.graph.GuardVerdict;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * The parent Negotiation must be in progress before any of its Resource Lifecycles will move.
 *
 * <p><b>What this ports, and what it does not.</b> There is a {@code NegotiationIsApprovedGuard} in
 * the codebase and it is <em>not</em> the source of this. That bean is attached to nothing — both
 * Resource configurations end with a transition fragment naming no source, no event and no target,
 * which Spring Statemachine discards silently, and all 21 Transitions of both committed graph dumps
 * record a null guard. It has never influenced a decision the application has made, and the
 * characterization suite says outright that a Guard which has never fired must not be
 * reimplemented.
 *
 * <p>What is live is imperative, in {@code ResourceLifecycleServiceImpl}: an early return of an
 * empty set unless the parent's current State equals {@code IN_PROGRESS}. So the <em>gate</em> is
 * ported here, and the class is not. That satisfies ADR 0005 and the characterization finding at
 * once, and is the only reading under which both are true.
 *
 * <p>Two things about the gate change in the move, and neither is this slab's to resolve — nothing
 * here is called from production code. Today it runs <em>before</em> any authority rule and yields
 * an absence; as a Guard it runs third and yields a refusal with a category. Both belong to the
 * cutover slab.
 *
 * <p>It reads the parent's State off the evaluation context, so it needs no port and does no I/O.
 * That is the whole reason the parent State is on the context rather than behind a lookup.
 *
 * <p>Wired as <b>one definition-level row per Resource-scope definition</b> — a null {@code
 * transition_id} — rather than one row per Transition, which is what removes the copy-drift risk of
 * re-attaching it to every Transition a later version adds. ADR 0005 rejected hardcoding it into
 * the evaluator for a reason worth restating: a Network may legitimately want Resource work to
 * begin before approval, so which Guards apply stays data.
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
