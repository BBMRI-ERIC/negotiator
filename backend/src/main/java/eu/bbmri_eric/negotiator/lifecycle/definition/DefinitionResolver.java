package eu.bbmri_eric.negotiator.lifecycle.definition;

/**
 * Definition Resolution: how new work finds the Definition Version it will run under, before it is
 * pinned.
 *
 * <p>The two scopes are separate methods because their inputs differ. A Negotiation has one family
 * to resolve. A Resource's family comes from an order of precedence over its own and its Networks'
 * associations, so its answer will depend on which Resource asks.
 *
 * <p>Resolution should always succeed, so both methods throw {@link DefinitionResolutionException}
 * when the configuration cannot name exactly one version, rather than returning an absence.
 *
 * <p>Both return the row id, because a caller writes it to a Definition Version Pin, which is a
 * {@code long}. Returning the entity would leak a package-private type and an association into the
 * definition graph.
 */
interface DefinitionResolver {

  /**
   * Answers {@link LifecycleDefinitions#resolveForNegotiation()}, whose javadoc states the
   * contract.
   */
  long resolveForNegotiation();

  /** Answers {@link LifecycleDefinitions#resolveForResource()}, on the same terms. */
  long resolveForResource();
}
