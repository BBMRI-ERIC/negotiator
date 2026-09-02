package eu.bbmri_eric.negotiator.lifecycle.evaluation;

import eu.bbmri_eric.negotiator.lifecycle.graph.EvaluationContext;
import eu.bbmri_eric.negotiator.lifecycle.graph.GuardVerdict;

/**
 * A self-describing Guard strategy: it declares its own catalogue key and its own params type, and
 * is folded into {@link GuardRegistry} at startup. New behaviour needs a new class implementing
 * this; new <em>configuration</em> needs only a Wiring row.
 *
 * <p>The two arguments of {@link #check} are the whole of ADR 0002's rule about where state comes
 * from. {@code params} is configuration, read from the Wiring row's jsonb once at compile time and
 * the same for every evaluation. {@code context} is runtime domain state, and is the only channel
 * for it — putting a negotiation id in {@code params} would make one Wiring row mean one
 * negotiation, which is not what a Definition Version is.
 *
 * @param <P> this strategy's params type. Use {@link NoParams} for a Guard that takes none.
 */
public interface Guard<P> {

  /**
   * The catalogue key Wiring rows name this strategy by. Must be unique across every Guard bean,
   * and a collision fails the boot rather than picking a winner.
   */
  String typeKey();

  /** The type this strategy's {@code params} jsonb is read into at compile time. */
  Class<P> paramsType();

  /** Whether the domain currently permits the move. Does no I/O and holds no state. */
  GuardVerdict check(EvaluationContext context, P params);
}
