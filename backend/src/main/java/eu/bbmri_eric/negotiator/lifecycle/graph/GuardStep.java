package eu.bbmri_eric.negotiator.lifecycle.graph;

/**
 * One Guard on a Compiled Graph, with its params already read into the strategy's type.
 *
 * <p>Compilation resolves the type key and the jsonb params once, so the Transition Evaluator never
 * sees JSON and cannot fail on it while firing.
 */
public interface GuardStep {

  /** The catalogue key this step was bound from. For messages, never for dispatch. */
  String typeKey();

  /** Whether the domain currently permits the move. Reads only what is on the context. */
  GuardVerdict check(EvaluationContext context);
}
