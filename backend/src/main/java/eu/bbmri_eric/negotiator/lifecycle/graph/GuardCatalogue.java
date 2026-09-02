package eu.bbmri_eric.negotiator.lifecycle.graph;

/**
 * The whole of what compiling a definition needs to know about the Guard catalogue: given a Wiring
 * row's type key and its raw jsonb params, hand back something callable.
 *
 * <p>One method, on purpose. ADR 0002 splits the subsystem so that "Guard and Action logic stays in
 * Java; only the Wiring is data", and this interface is that split made into a seam. The compiler
 * lives in the definition package and knows about rows; the registry lives in the evaluation
 * package and knows about strategy beans, params types and Jackson. Neither package names the
 * other, and neither one grows a reason to.
 *
 * <p>It also fixes <em>when</em> a bad definition is discovered. An unknown type key or params that
 * do not fit the declared type are refused here, while a graph is being compiled — not at fire
 * time, in front of a user, on whichever Transition happens to be tried first.
 */
public interface GuardCatalogue {

  /**
   * Binds one Guard Wiring row to its strategy.
   *
   * @param typeKey the row's {@code type_key}
   * @param paramsJson the row's {@code params}, or null — which is legal and ordinary, since a
   *     strategy that takes no parameters needs none
   * @throws IllegalArgumentException if no strategy declares that key, or if the params do not read
   *     into the strategy's declared type
   */
  GuardStep bind(String typeKey, String paramsJson);
}
