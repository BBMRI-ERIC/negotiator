package eu.bbmri_eric.negotiator.lifecycle.evaluation;

import eu.bbmri_eric.negotiator.lifecycle.graph.ActionContext;

/**
 * A self-describing Action strategy: it declares its own catalogue key and its own params type, and
 * is folded into {@link ActionRegistry} at startup. The Action-side twin of {@link Guard}, with the
 * same shape for the same reasons.
 *
 * <p>Giving Actions params rather than only Guards is what collapses today's three post-visibility
 * Action classes into one type key with a scope and a flag — ADR 0002's worked example of the whole
 * split, and the reason this interface is generic at all.
 *
 * @param <P> this strategy's params type. Use {@link NoParams} for an Action that takes none.
 */
public interface Action<P> {

  /** The catalogue key Wiring rows name this strategy by. Unique across every Action bean. */
  String typeKey();

  /** The type this strategy's {@code params} jsonb is read into at compile time. */
  Class<P> paramsType();

  /** Runs the effect, after the move has committed. */
  void run(ActionContext context, P params);
}
