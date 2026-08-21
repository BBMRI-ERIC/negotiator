package eu.bbmri_eric.negotiator.lifecycle.definition;

/**
 * Definition Resolution: how new work finds the Definition Version it will run under, before it is
 * pinned.
 *
 * <p>The two scopes are separate methods because their inputs differ. A Negotiation has one family
 * to resolve. A Resource's family comes from an order of precedence over its own and its Networks'
 * associations, so its answer will depend on which Resource asks.
 *
 * <p>Resolution is meant to be total: exactly one family carries the Global Default flag, which is
 * what lets the Spawn of a Negotiation's Resource Lifecycles either initialize all of them or none.
 * So both methods return a definition and throw when the configuration cannot name exactly one,
 * rather than handing back an absence for a caller to branch on. Until the definitions are seeded
 * there are none at all, and throwing is the only thing either method can do.
 */
interface DefinitionResolver {

  /**
   * The active Definition Version of the sole Negotiation-scope Definition Family.
   *
   * @throws DefinitionResolutionException if no such version exists, or if more than one family has
   *     an active Negotiation-scope version
   */
  LifecycleDefinition resolveForNegotiation();

  /**
   * The active Definition Version of the Global Default Family. Takes no Resource: the precedence
   * walk over a Resource's own associations and its Networks' is not built, and until it is there
   * is nothing a Resource could change about the answer.
   *
   * @throws DefinitionResolutionException if the Global Default Family has no active version, or if
   *     no family carries the flag
   */
  LifecycleDefinition resolveForResource();
}
