package eu.bbmri_eric.negotiator.lifecycle.definition;

import eu.bbmri_eric.negotiator.lifecycle.graph.CompiledGraph;
import eu.bbmri_eric.negotiator.lifecycle.graph.InvalidGraphException;
import java.util.Collection;
import java.util.Map;

/**
 * The definition package, as everything outside it sees it: which Definition Version new work
 * resolves to, and the Compiled Graph of a Definition Version already pinned to work in flight.
 *
 * <p>This is the whole of the package's public surface. Loading a version's rows, Definition
 * Compilation and caching one graph per version all sit behind these four methods, and every type
 * they are done with — the six entities, their repositories, the compiler, the cache — stays
 * package private. A caller holds a {@code long} and gets back a {@link CompiledGraph}; there is
 * nothing here to wire together and no order to call things in.
 *
 * <p><b>Why an interface at all, when the cache deliberately refused one.</b> {@link
 * CompiledGraphCache} argues that "a port with one adapter is indirection; the interface arrives
 * when a second adapter does", and that argument stands — it is about a load port <em>inside</em>
 * this module, and no such port exists. This is the other thing: the module's own edge. It has to
 * be an interface rather than a class because the package-private types below it are not
 * expressible in a public signature, and because the module that fires Events is held behind a test
 * double of exactly this shape rather than behind a database.
 *
 * <p><b>Everything is keyed on the Definition Version's row id alone</b> (ADR 0003). No {@code
 * (family, version)} pair appears in any signature here, and Definition Resolution answers in ids
 * for the same reason: a caller resolving a version in order to pin it never needs to name a row,
 * so the entity stays inside.
 */
public interface LifecycleDefinitions {

  /**
   * The Definition Version a new Negotiation's Lifecycle runs under, to be recorded as its
   * Definition Version Pin.
   *
   * @return the row id of the active version of the sole Negotiation-scope Definition Family
   * @throws DefinitionResolutionException if no such version exists, or if more than one family has
   *     an active Negotiation-scope version
   */
  long resolveForNegotiation();

  /**
   * The Definition Version a Resource's Lifecycle runs under, to be recorded as its Definition
   * Version Pin at Spawn.
   *
   * <p>Takes no Resource yet: the precedence walk over a Resource's own associations and its
   * Networks' is stage 2 (ADR 0004), and until it exists there is nothing a Resource could change
   * about the answer. That walk adds the parameter here as well as in the implementation — it is a
   * signature change, which is why it is filed as stage 2's rather than smuggled in now.
   *
   * @return the row id of the active version of the Global Default Family
   * @throws DefinitionResolutionException if the Global Default Family has no active version, or if
   *     no family carries the flag
   */
  long resolveForResource();

  /**
   * The Compiled Graph of one Definition Version: its rows loaded, compiled, and kept, so that the
   * second caller to ask for a version pays for none of it.
   *
   * <p>A version that cannot compile fails here every time it is asked for, rather than once —
   * nothing caches a failure, so a Definition Version repaired after a bad compile is served
   * without a restart.
   *
   * @param definitionVersionId the row id of the version, typically a Definition Version Pin
   * @throws InvalidGraphException if no such version exists, or if its rows do not form a graph
   */
  CompiledGraph graphFor(long definitionVersionId);

  /**
   * The Compiled Graphs of several Definition Versions at once, keyed by version id.
   *
   * <p>This is what a Negotiation-scope evaluation needs, and the reason the cache exists at all:
   * terminality has to be asked of each Resource's <em>own</em> pinned version, so assembling one
   * context means holding one graph per Resource. Duplicates are the normal case — Resources
   * sharing a version share a graph — and each distinct version is loaded and compiled at most once
   * however many ids name it. That is ADR 0001's N-way load, answered here rather than discovered
   * under load in a caller's loop.
   *
   * <p>It is a method rather than a documented loop so that it can stop being one: nothing in the
   * signature promises a query per version, and a later implementation may read several versions'
   * rows together without any caller changing.
   *
   * @param definitionVersionIds the version ids to resolve; may repeat, and may be empty
   * @return one entry per <em>distinct</em> id, so the map is at most as large as the collection
   * @throws InvalidGraphException if any named version does not exist or cannot be compiled
   */
  Map<Long, CompiledGraph> graphsFor(Collection<Long> definitionVersionIds);
}
