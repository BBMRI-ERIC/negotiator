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
 * Delegates Definition Resolution and produces one Compiled Graph per Definition Version by loading,
 * compiling and caching it.
 *
 * <p>It builds the compiler and the cache itself rather than injecting them, so no caller can wire
 * a second one. It injects the catalogues because the graph package declares them and the
 * evaluation package implements them.
 *
 * <p>These methods open no transaction. The loader opens its own, and a transaction here would cost
 * one per cache hit.
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
   * One graph per distinct id. The cache already loads a repeated id once. {@code distinct} is there
   * because a map cannot hold duplicate keys.
   */
  @Override
  public Map<Long, CompiledGraph> graphsFor(Collection<Long> definitionVersionIds) {
    definitionVersionIds.forEach(LifecycleDefinitionsImpl::requireAnId);
    return definitionVersionIds.stream()
        .distinct()
        .collect(Collectors.toUnmodifiableMap(id -> id, this::graphFor));
  }

  /**
   * A null id means a Lifecycle with no Definition Version Pin, which the nullable pin columns
   * allow. This names the cause, where an unboxing exception would not.
   *
   * <p>It checks the whole collection before resolving any id, so a refused batch issues no
   * queries.
   */
  private static void requireAnId(Long definitionVersionId) {
    Objects.requireNonNull(
        definitionVersionId,
        "A null Definition Version id was asked for. Some Lifecycle in this set carries no"
            + " Definition Version Pin, and an unpinned Lifecycle has no graph to be judged"
            + " against.");
  }
}
