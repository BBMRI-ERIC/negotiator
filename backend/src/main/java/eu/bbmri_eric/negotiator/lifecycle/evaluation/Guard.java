package eu.bbmri_eric.negotiator.lifecycle.evaluation;

import eu.bbmri_eric.negotiator.lifecycle.graph.EvaluationContext;
import eu.bbmri_eric.negotiator.lifecycle.graph.GuardVerdict;

/**
 * A Guard strategy that declares its own catalogue key and params type. {@link GuardRegistry}
 * collects every one at startup. New behaviour needs a new class; new configuration needs only a
 * Wiring row.
 *
 * <p>{@code params} is configuration, read once at compile time and the same for every evaluation.
 * {@code context} is the only channel for runtime domain state. A negotiation id in {@code params}
 * would tie a Wiring row, and so a Definition Version, to one negotiation.
 *
 * @param <P> this strategy's params type. Use {@link NoParams} for a Guard that takes none.
 */
public interface Guard<P> {

  /**
   * The catalogue key Wiring rows name this strategy by. Must be unique across every Guard bean. A
   * collision fails the boot.
   */
  String typeKey();

  /** The type this strategy's {@code params} jsonb is read into at compile time. */
  Class<P> paramsType();

  /** Whether the domain currently permits the move. Does no I/O and holds no state. */
  GuardVerdict check(EvaluationContext context, P params);
}
