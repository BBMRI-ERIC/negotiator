package eu.bbmri_eric.negotiator.lifecycle.definition;

import eu.bbmri_eric.negotiator.lifecycle.graph.ActionCatalogue;
import eu.bbmri_eric.negotiator.lifecycle.graph.CompiledGraph;
import eu.bbmri_eric.negotiator.lifecycle.graph.GuardCatalogue;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * The definition package assembled: Definition Resolution delegated, and one graph per Definition
 * Version produced by loading its rows, compiling them, and keeping the result.
 *
 * <p><b>The compiler and the cache are built here rather than injected.</b> Both are internals of
 * this module, not collaborators a caller — or a test, or a future second bean — gets to choose,
 * and making either a bean of its own would be an invitation to wire a second one. The cache in
 * particular cannot be a bean without inventing something for its producer to be: it takes the
 * load-and-compile function directly, on the argument that "a port with one adapter is
 * indirection". That function is the two lines below, and this constructor is the production
 * adapter its javadoc has been describing as the cutover's to write.
 *
 * <p>The catalogues are injected because they genuinely are seams — {@link GuardCatalogue} and
 * {@link ActionCatalogue} are declared in the graph package and implemented in the evaluation
 * package, and are the one place these three packages meet.
 *
 * <p><b>No transaction here.</b> The loader owns one, and it has to be entered through a bean
 * boundary to exist at all; putting one on these methods instead would open a transaction for every
 * cache hit, which is the cost the cache exists to remove.
 */
@Service
class LifecycleDefinitionsImpl implements LifecycleDefinitions {

  private final DefinitionResolver resolution;
  private final CompiledGraphCache graphs;

  LifecycleDefinitionsImpl(
      DefinitionResolver resolution,
      DefinitionVersionLoader loader,
      GuardCatalogue guardCatalogue,
      ActionCatalogue actionCatalogue) {
    this.resolution = resolution;
    DefinitionCompiler compiler = new DefinitionCompiler(guardCatalogue, actionCatalogue);
    this.graphs = new CompiledGraphCache(id -> compiler.compile(loader.load(id)));
  }

  @Override
  public long resolveForNegotiation() {
    return resolution.resolveForNegotiation();
  }

  @Override
  public long resolveForResource() {
    return resolution.resolveForResource();
  }

  @Override
  public CompiledGraph graphFor(long definitionVersionId) {
    return graphs.graphFor(definitionVersionId);
  }

  /**
   * One graph per <em>distinct</em> id. The cache would already collapse a repeated id to one load,
   * so {@code distinct} is not what makes the guarantee true — it is what makes the answer a map at
   * all, since three Resources on two versions produce three ids and two entries.
   */
  @Override
  public Map<Long, CompiledGraph> graphsFor(Collection<Long> definitionVersionIds) {
    definitionVersionIds.forEach(LifecycleDefinitionsImpl::requireAnId);
    return definitionVersionIds.stream()
        .distinct()
        .collect(Collectors.toUnmodifiableMap(id -> id, this::graphFor));
  }

  /**
   * A null among the ids is an unpinned Lifecycle, not a version that failed to compile. Both pin
   * columns are still nullable — the migration that fills them in and sets them {@code NOT NULL} is
   * the cutover's — so a caller collecting pins off a Negotiation's Resources can hold one, and the
   * unboxing exception it would otherwise cause names neither the collection nor the reason.
   *
   * <p>Checked over the whole collection before any of it is resolved, rather than as each id
   * arrives at the cache. Nothing would be left inconsistent either way — a resolution writes
   * nothing — but a batch that refuses its input has not started the six queries per version it was
   * about to issue, and which ids happened to be resolved first stops being part of what a caller
   * sees.
   */
  private static void requireAnId(Long definitionVersionId) {
    Objects.requireNonNull(
        definitionVersionId,
        "A null Definition Version id was asked for. Some Lifecycle in this set carries no"
            + " Definition Version Pin, and an unpinned Lifecycle has no graph to be judged"
            + " against.");
  }
}
