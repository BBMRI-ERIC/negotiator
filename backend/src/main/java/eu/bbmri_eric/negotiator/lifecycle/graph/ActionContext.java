package eu.bbmri_eric.negotiator.lifecycle.graph;

import java.util.Objects;

/**
 * What an Action is told when it runs: the same domain facts that gated the move, plus the edge
 * that committed.
 *
 * <p>The second half is why this is not simply an {@link EvaluationContext}. A Guard runs before a
 * commit and asks whether a move is legal; an Action runs after one and acts on what happened, so
 * it needs to know which Transition fired — {@code committed.toState()} is where the Lifecycle now
 * is, and {@code evaluation.subject().currentState()} is where it was.
 *
 * <p>This shape is a guess, and the slab that actually runs Actions owns it. Nothing here runs one:
 * ADR 0001 puts committing a move and running its Actions in the services around the evaluator, so
 * a permitted outcome reports its Action chain and stops. What the shape is good for today is
 * proving that a params-carrying Action can be configured, bound and handed a negotiation to act on
 * without the evaluator learning anything about it.
 */
public record ActionContext(EvaluationContext evaluation, CompiledTransition committed) {

  public ActionContext {
    Objects.requireNonNull(evaluation, "evaluation");
    Objects.requireNonNull(committed, "committed");
  }
}
