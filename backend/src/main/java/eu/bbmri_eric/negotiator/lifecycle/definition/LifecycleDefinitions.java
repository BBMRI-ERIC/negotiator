package eu.bbmri_eric.negotiator.lifecycle.definition;

import eu.bbmri_eric.negotiator.lifecycle.graph.CompiledGraph;
import eu.bbmri_eric.negotiator.lifecycle.graph.InvalidGraphException;
import java.util.Collection;
import java.util.Map;

/**
 * The definition package's only public type: which Definition Version new work resolves to, and
 * the Compiled Graph of a Definition Version already pinned.
 *
 * <p>Loading rows, Definition Compilation and caching all sit behind these methods, and every other
 * type in the package is package-private. It is an interface so that Event Firing tests can use a
 * test double instead of a database.
 *
 * <p>Everything is keyed on the Definition Version's row id, because the row id is the only
 * identity of a version.
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
   * <p>Takes no Resource, because every Resource resolves to the Global Default Family until
   * Resources and Networks can be associated with a family.
   *
   * @return the row id of the active version of the Global Default Family
   * @throws DefinitionResolutionException if the Global Default Family has no active version, or if
   *     no family carries the flag
   */
  long resolveForResource();

  /**
   * The Compiled Graph of one Definition Version, loaded and compiled on first request and cached.
   *
   * <p>A failed compile is not cached, so a Definition Version repaired after a bad compile is
   * served without a restart.
   *
   * @param definitionVersionId the row id of the version, typically a Definition Version Pin
   * @throws InvalidGraphException if no such version exists, or if its rows do not form a graph
   */
  CompiledGraph graphFor(long definitionVersionId);

  /**
   * The Compiled Graphs of several Definition Versions at once, keyed by version id.
   *
   * <p>A Negotiation-scope evaluation needs one graph per Resource, because terminality is asked of
   * each Resource's own pinned version. Ids often repeat, and each distinct version is loaded and
   * compiled at most once. The signature does not promise a query per version, so a later
   * implementation may load several versions together.
   *
   * @param definitionVersionIds the version ids to resolve; may repeat, and may be empty
   * @return one entry per <em>distinct</em> id, so the map is at most as large as the collection
   * @throws InvalidGraphException if any named version does not exist or cannot be compiled
   */
  Map<Long, CompiledGraph> graphsFor(Collection<Long> definitionVersionIds);
}
