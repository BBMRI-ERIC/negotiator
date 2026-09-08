package eu.bbmri_eric.negotiator.lifecycle.evaluation;

import eu.bbmri_eric.negotiator.lifecycle.graph.ActionContext;

/**
 * An Action strategy that declares its own catalogue key and params type. {@link ActionRegistry}
 * collects every one at startup.
 *
 * <p>The interface is generic so that one strategy can cover several Wirings through params. For
 * example, one post-visibility Action takes a scope and a flag instead of being three classes.
 *
 * @param <P> this strategy's params type. Use {@link NoParams} for an Action that takes none.
 */
public interface Action<P> {

  /** The catalogue key Wiring rows name this strategy by. Unique across every Action bean. */
  String typeKey();

  /** The type this strategy's {@code params} jsonb is read into at compile time. */
  Class<P> paramsType();

  /** Runs after the Transition has committed. */
  void run(ActionContext context, P params);
}
