package eu.bbmri_eric.negotiator.lifecycle.definition;

import eu.bbmri_eric.negotiator.lifecycle.graph.InvalidGraphException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads one Definition Version's six tables and hands back {@link DefinitionVersionRows}. The other
 * half of the "explicit, testable step" ADR 0001 asked for, and the half that had no implementation
 * until now: the compiler has always been handed rows, and nothing produced any.
 *
 * <p>Six queries, one per table, in a fixed order. Not a single join: a definition is one row, its
 * vertices and edges are independent collections, and one join across all six would multiply every
 * State by every Guard Wiring for a reader that immediately splits them apart again.
 *
 * <p><b>One transaction, and that is load-bearing rather than tidy.</b> {@link DefinitionCompiler}
 * compares a row's owning version against {@link DefinitionVersionRows#definition()} by reference
 * and groups Guard and Action Wiring by {@link Transition} identity — "within one persistence
 * context a Transition is one instance", as it says. Six calls without a transaction around them
 * are six persistence contexts, and every one of those comparisons would then be against a
 * different instance of the same row: the compile would refuse a perfectly good version as
 * straddling two. A {@code @DataJpaTest} cannot catch that, because it wraps each test in a
 * transaction of its own and supplies the guarantee for free — which is why the test that proves it
 * is an integration test that commits.
 *
 * <p>The Definition Version itself is read first, so that every {@code lifecycle_definition_id}
 * below resolves to the instance it returned rather than to a proxy created on the way past.
 *
 * <p><b>Everything the compiler will dereference is initialized here</b>, by the fetch joins on the
 * three queries that have associations worth following. Compilation happens after this transaction
 * commits — it is pure, and holding a connection open across it buys nothing — so an association
 * left lazy would not be a slow path but a {@code LazyInitializationException} on a detached row.
 */
@Component
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class DefinitionVersionLoader {

  private final LifecycleDefinitionRepository definitions;
  private final StateRepository states;
  private final EventRepository events;
  private final TransitionRepository transitions;
  private final GuardWiringRepository guardWirings;
  private final ActionWiringRepository actionWirings;

  /**
   * Every row of the named Definition Version.
   *
   * <p>Public on a package-private class, which is not a widening: Spring's proxy-based
   * {@code @Transactional} is applied to public methods only, and a package-private one would be
   * silently ignored — leaving the six queries in six transactions and the identity guarantee above
   * gone. Nothing outside this package can name the class to call it.
   *
   * @throws InvalidGraphException if no Definition Version has that id. A caller reaching this with
   *     an id is a Lifecycle holding a Definition Version Pin that no longer names a row, which is
   *     graph corruption of the same kind as a State the pinned version never declared — not an
   *     absence for a caller to branch on.
   */
  @Transactional(readOnly = true)
  public DefinitionVersionRows load(long definitionVersionId) {
    LifecycleDefinition definition =
        definitions
            .findById(definitionVersionId)
            .orElseThrow(
                () ->
                    new InvalidGraphException(
                        ("No Lifecycle Definition has id %d, so no graph can be compiled for it. "
                                + "Work pinned to that Definition Version cannot be judged at all.")
                            .formatted(definitionVersionId)));
    return new DefinitionVersionRows(
        definition,
        states.findByLifecycleDefinitionIdOrderById(definitionVersionId),
        events.findByLifecycleDefinitionIdOrderById(definitionVersionId),
        transitions.findByLifecycleDefinitionIdOrderById(definitionVersionId),
        guardWirings.findByLifecycleDefinitionIdOrderById(definitionVersionId),
        actionWirings.findByTransitionLifecycleDefinitionIdOrderById(definitionVersionId));
  }
}
