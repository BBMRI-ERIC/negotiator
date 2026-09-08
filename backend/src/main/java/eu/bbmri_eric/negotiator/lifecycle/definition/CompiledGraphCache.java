package eu.bbmri_eric.negotiator.lifecycle.definition;

import eu.bbmri_eric.negotiator.lifecycle.graph.CompiledGraph;
import eu.bbmri_eric.negotiator.lifecycle.graph.InvalidGraphException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongFunction;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

/**
 * One compiled graph per Definition Version, produced on first request and then kept.
 *
 * <p>Its whole reason to exist is that terminality has to be asked of each Resource's <em>own</em>
 * pinned Definition Version. A caller assembling a Negotiation-scope evaluation context therefore
 * resolves one graph per Resource before it calls the evaluator — which is the explicit, testable
 * load step ADR 0001 asked for in place of an engine that owns its persistence, and equally the
 * N-way load that ADR warned would otherwise be discovered under load. Without this cache that
 * assembly recompiles the same version once per Resource.
 *
 * <p><b>Keyed on the Definition Version's row id alone.</b> No {@code (family, version)} key exists
 * anywhere in this subsystem, and there is no join between a pinned Lifecycle and its graph: ADR
 * 0003 makes the row id the sole machine identity, and a cache keyed any other way would quietly
 * reintroduce the composite lookup that ADR removed. The Version sequence is a display label, and
 * nothing here displays anything.
 *
 * <p><b>Unbounded, deliberately.</b> A published version is immutable and is retained for as long
 * as any work is pinned to it, so there is nothing to expire; and a size policy needs production
 * cardinality evidence that nobody has yet. What it supports instead is targeted invalidation:
 * publishing a new version evicts that version's entry and leaves every other pinned version
 * untouched.
 *
 * <p><b>No load port.</b> The producer is named directly, as a function of a row id, rather than
 * through an interface of this subsystem's own. A port with one adapter is indirection; the
 * interface arrives when a second adapter does. The producer the cutover slab will pass loads a
 * version's rows and compiles them; nothing in production passes one yet, because that load reads
 * the definition tables and is the cutover's to write.
 *
 * <p><b>No {@code computeIfAbsent}.</b> That would run the producer inside the map's per-bin lock —
 * harmless against a test lambda, wrong against the real producer, which will issue six queries in
 * a transaction. So this reads, produces <em>outside</em> the map, then puts if absent. Two threads
 * may compile one version twice; compilation is pure and idempotent, so a race duplicates work
 * rather than producing a wrong answer, which is the right trade against a lock held across I/O.
 * Both callers are still handed the one graph that won the put, so a version has one graph however
 * many were compiled.
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
   * Drops one Definition Version's entry, for the publish of a new version of its family. Every
   * other version's entry is left alone, which is what keeps work pinned to an older version
   * undisturbed by a publish. Invalidating a version never asked for is a no-op.
   */
  void invalidate(long definitionVersionId) {
    graphs.remove(definitionVersionId);
  }

  /**
   * The one corruption a row-id key cannot rule out by itself. A producer asked for one version
   * that reads another's rows returns a graph that is internally consistent, so the compiler's own
   * version-mixing refusal sees nothing wrong with it, and only this method knows which version was
   * asked for. Filing it under the id asked for would serve one version's graph to work pinned to
   * another — the failure the Definition Version Pin exists to prevent — and would do it silently,
   * for as long as the entry lived.
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
