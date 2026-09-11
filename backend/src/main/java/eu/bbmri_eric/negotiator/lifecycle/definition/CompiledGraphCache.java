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
 * <p>It avoids {@code computeIfAbsent}, which would run the producer, and its queries, inside the
 * map's per-bin lock. It reads, produces outside the map, then puts if absent. Two threads may
 * compile one version twice. Compilation is pure, so the race only duplicates work, and both
 * callers get the graph that won the put.
 *
 * <p>It does not cache a failed production, so a Definition Version fixed after a bad compile is
 * served without a restart. {@link InvalidGraphException} passes through unwrapped.
 *
 * <p>Callers outside the package use {@link LifecycleDefinitions#graphFor}, which already holds the
 * producer.
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
