package eu.bbmri_eric.negotiator.lifecycle.graph;

/**
 * One Guard on a compiled graph, with its configuration already applied.
 *
 * <p>This is what "compiled" buys. A Guard Wiring row is a type key and a jsonb blob; a {@code
 * GuardStep} is a thing you can call. Resolving the key against the catalogue and reading the blob
 * into the strategy's declared params type both happen once, when the graph is compiled — so the
 * evaluator never sees a type key, never sees JSON, and cannot fail on either at fire time. ADR
 * 0002 asks for the params to be "deserialized into the strategy's declared type at load time";
 * taking that literally is what makes the pipeline a loop over closures.
 *
 * <p>{@link #typeKey} survives the binding only so that a refusal and a log line can name what
 * refused. Nothing dispatches on it.
 */
public interface GuardStep {

  /** The catalogue key this step was bound from. For messages, never for dispatch. */
  String typeKey();

  /** Whether the domain currently permits the move. Reads only what is on the context. */
  GuardVerdict check(EvaluationContext context);
}
