package eu.bbmri_eric.negotiator.lifecycle.definition;

import eu.bbmri_eric.negotiator.lifecycle.graph.CompiledGraph;
import eu.bbmri_eric.negotiator.lifecycle.graph.InvalidGraphException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongFunction;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

/**
 * One Compiled Graph per Definition Version, produced on first request and then kept.
 *
 * <p>Terminality has to be asked of each Resource's own pinned Definition Version, so assembling a
 * Negotiation-scope evaluation resolves one graph per Resource. Without this cache that assembly
 * recompiles the same version once per Resource.
 *
 * <p>It is keyed on the Definition Version's row id alone, because the row id is the only identity
 * of a version. The Version sequence is a display label.
 *
 * <p>It is unbounded. A Definition Version is immutable and kept while work is pinned to it, so
 * nothing expires. Publishing a new version evicts only that version's entry.
 *
 * <p><b>No load port.</b> The producer is named directly, as a function of a row id, rather than
 * through an interface of this subsystem's own. A port with one adapter is indirection; the
 * interface arrives when a second adapter does. The producer the cutover slab will pass loads a
 * version's rows and compiles them; nothing in production passes one yet, because that load reads
 * the definition tables and is the cutover's to write.
 *
 * <p>It does not cache a failed production, so a Definition Version fixed after a bad compile is
 * served without a restart. {@link InvalidGraphException} passes through unwrapped.
 *
 * <p>A failed production is not cached, which is what lets a Definition Version fixed after a bad
 * compile be served without a restart. {@link InvalidGraphException} passes through unwrapped, so a
 * caller catching graph corruption still catches it through the cache.
 *
 * <p>It is package-private on purpose. Nothing outside this package may resolve a graph yet, which
 * is what keeps the package inert while there is no loader to produce one.
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class CompiledGraphCache {

  private final LongFunction<CompiledGraph> producer;

  private final Map<Long, CompiledGraph> graphs = new ConcurrentHashMap<>();

  /**
   * The graph of one Definition Version, compiled on the first request for it and reused after.
   *
   * @throws InvalidGraphException if the graph cannot be produced, or is not this version's
   */
  CompiledGraph graphFor(long definitionVersionId) {
    CompiledGraph cached = graphs.get(definitionVersionId);
    if (cached != null) {
      return cached;
    }
    CompiledGraph produced = producer.apply(definitionVersionId);
    requireTheVersionAskedFor(definitionVersionId, produced);
    CompiledGraph won = graphs.putIfAbsent(definitionVersionId, produced);
    return won != null ? won : produced;
  }

  /**
   * Drops one Definition Version's entry when a new version of its family is published. Other
   * entries stay, so work pinned to an older version is not disturbed. Invalidating a version never
   * asked for does nothing.
   */
  void invalidate(long definitionVersionId) {
    graphs.remove(definitionVersionId);
  }

  /**
   * Refuses a graph built from another version's rows. Such a graph is internally consistent, so
   * the compiler accepts it, and only this method knows which version was asked for. Caching it
   * would silently serve one version's graph to work pinned to another.
   */
  private static void requireTheVersionAskedFor(long definitionVersionId, CompiledGraph produced) {
    if (produced.definitionVersionId() != definitionVersionId) {
      throw new InvalidGraphException(
          ("A graph was asked for Definition Version %d and produced for %d. Its producer read the "
                  + "wrong version's rows, and caching it under the id asked for would serve one "
                  + "version's graph to a Lifecycle pinned to another.")
              .formatted(definitionVersionId, produced.definitionVersionId()));
    }
  }
}
