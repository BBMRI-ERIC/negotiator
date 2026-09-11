package eu.bbmri_eric.negotiator.lifecycle.definition;

import static eu.bbmri_eric.negotiator.lifecycle.definition.DefinitionFixtures.STANDARD_FAMILY;
import static eu.bbmri_eric.negotiator.lifecycle.definition.DefinitionFixtures.bindingAnyAction;
import static eu.bbmri_eric.negotiator.lifecycle.definition.DefinitionFixtures.bindingAnyGuard;
import static eu.bbmri_eric.negotiator.lifecycle.definition.DefinitionFixtures.stateBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import eu.bbmri_eric.negotiator.lifecycle.graph.CompiledGraph;
import eu.bbmri_eric.negotiator.lifecycle.graph.InvalidGraphException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * What the package's public interface promises about <em>how often</em> it does its work, with the
 * loader mocked so that every load is counted and none of them touches a database.
 *
 * <p>This is where the caching criteria live rather than in the integration test, and the reason is
 * that a load is only observable by counting. Two resolutions of one version return the same graph
 * whether it was compiled once or twice — the cache hands both callers whichever production won the
 * put — so "the same instance came back" is necessary and nowhere near sufficient. A mock counts.
 *
 * <p>{@link CompiledGraphCacheTest} makes the same claims one layer down, against a cache holding a
 * lambda. What is added here is that the production wiring actually reaches it: nothing but this
 * test would notice a {@code graphsFor} that resolved each id through a fresh cache, or a
 * constructor that built one cache per call.
 */
class LifecycleDefinitionsImplTest {

  private static final long A_VERSION = 11L;
  private static final long ANOTHER_VERSION = 12L;

  /** Three Resources across two versions: the shape ADR 0001's N-way load warning is about. */
  private static final List<Long> THREE_RESOURCES_TWO_VERSIONS =
      List.of(A_VERSION, ANOTHER_VERSION, A_VERSION);

  private final DefinitionResolver resolution = mock(DefinitionResolver.class);
  private final DefinitionVersionLoader loader = mock(DefinitionVersionLoader.class);

  private final LifecycleDefinitions definitions =
      new LifecycleDefinitionsImpl(resolution, loader, bindingAnyGuard(), bindingAnyAction());

  @Test
  @DisplayName("a version asked for twice is loaded and compiled once, and answered identically")
  void graphFor_loadsAndCompilesOncePerVersion() {
    loaderAnswersFor(A_VERSION);

    CompiledGraph first = definitions.graphFor(A_VERSION);
    CompiledGraph second = definitions.graphFor(A_VERSION);

    verify(loader, times(1)).load(A_VERSION);
    assertThat(second).isSameAs(first);
  }

  /**
   * The slab's headline number: three Resources across two versions is two loads, not three.
   *
   * <p>Both halves matter. The map has one entry per <em>distinct</em> version, because that is
   * what a caller pairing each Resource with its own graph looks its pin up in; and the repeated id
   * cost nothing, because the whole reason a Negotiation-scope evaluation resolves per Resource is
   * that terminality has to be asked of each Resource's own pinned version, and most of them share
   * one.
   */
  @Test
  @DisplayName("a set of ids compiles each distinct version once, however often it is named")
  void graphsFor_compilesEachDistinctVersionOnce() {
    loaderAnswersFor(A_VERSION);
    loaderAnswersFor(ANOTHER_VERSION);

    Map<Long, CompiledGraph> graphs = definitions.graphsFor(THREE_RESOURCES_TWO_VERSIONS);

    verify(loader, times(1)).load(A_VERSION);
    verify(loader, times(1)).load(ANOTHER_VERSION);
    verifyNoMoreInteractions(loader);
    assertThat(graphs).hasSize(2).containsOnlyKeys(A_VERSION, ANOTHER_VERSION);
    assertThat(graphs.get(A_VERSION).definitionVersionId()).isEqualTo(A_VERSION);
    assertThat(graphs.get(ANOTHER_VERSION).definitionVersionId()).isEqualTo(ANOTHER_VERSION);
  }

  /** One cache, not one per call: a version already resolved is not loaded again for a batch. */
  @Test
  @DisplayName("a batch reuses what an earlier resolution already compiled")
  void graphsFor_reusesGraphsAlreadyHeld() {
    loaderAnswersFor(A_VERSION);
    loaderAnswersFor(ANOTHER_VERSION);
    CompiledGraph alreadyHeld = definitions.graphFor(A_VERSION);

    Map<Long, CompiledGraph> graphs = definitions.graphsFor(THREE_RESOURCES_TWO_VERSIONS);

    verify(loader, times(1)).load(A_VERSION);
    assertThat(graphs.get(A_VERSION)).isSameAs(alreadyHeld);
  }

  @Test
  @DisplayName("a Negotiation with no Resources resolves no graphs and loads nothing")
  void graphsFor_ofNoIds_isEmpty() {
    assertThat(definitions.graphsFor(List.of())).isEmpty();
    verifyNoInteractions(loader);
  }

  /**
   * Both pin columns are still nullable until the cutover backfills them, so a caller collecting
   * pins off a Negotiation's Resources can hold a null. What it must not get back is an unboxing
   * failure naming neither the collection nor the reason.
   *
   * <p>The null is second in the list on purpose, and nothing is loaded even so: the batch is
   * checked before it is resolved, so which ids happened to come before the bad one is not part of
   * what the caller sees.
   */
  @Test
  @DisplayName("an unpinned Lifecycle in the set is refused before anything is loaded")
  void graphsFor_whenAnIdIsMissing_namesTheUnpinnedLifecycle() {
    assertThatThrownBy(() -> definitions.graphsFor(Arrays.asList(A_VERSION, null)))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("Definition Version Pin");

    verifyNoInteractions(loader);
  }

  /**
   * A definition repaired after a bad compile is served without a restart. The cache's own test
   * makes this claim against a lambda; what is under test here is that the repair is
   * <em>reread</em> — the second call must reach the loader, not be answered from a remembered
   * failure.
   */
  @Test
  @DisplayName("a version that could not compile is loaded again next time it is asked for")
  void graphFor_doesNotCacheAFailure() {
    when(loader.load(A_VERSION))
        .thenThrow(new InvalidGraphException("Definition Version 11 declares no initial State"))
        .thenReturn(rowsFor(A_VERSION));

    assertThatThrownBy(() -> definitions.graphFor(A_VERSION))
        .isInstanceOf(InvalidGraphException.class);

    assertThat(definitions.graphFor(A_VERSION).definitionVersionId()).isEqualTo(A_VERSION);
    verify(loader, times(2)).load(A_VERSION);
  }

  /**
   * Resolution is delegated whole, and the answer is a row id. Thin, and here because it is the
   * only place the package's public surface and its internal resolver are seen to be the same two
   * questions — a {@code resolveForResource} that quietly answered the Negotiation question would
   * fail nothing else in this suite.
   */
  @Test
  @DisplayName("both Definition Resolution questions reach the resolver and answer in row ids")
  void resolution_isDelegated() {
    when(resolution.resolveForNegotiation()).thenReturn(A_VERSION);
    when(resolution.resolveForResource()).thenReturn(ANOTHER_VERSION);

    assertThat(definitions.resolveForNegotiation()).isEqualTo(A_VERSION);
    assertThat(definitions.resolveForResource()).isEqualTo(ANOTHER_VERSION);
    verifyNoInteractions(loader);
  }

  private void loaderAnswersFor(long definitionVersionId) {
    when(loader.load(definitionVersionId)).thenReturn(rowsFor(definitionVersionId));
  }

  /**
   * The smallest thing that compiles: one Definition Version and the one initial State a graph must
   * have. Nothing here is about what a graph contains — {@link DefinitionCompilerTest} owns that —
   * only about which version it says it is, since that is what the answers are keyed on.
   */
  private static DefinitionVersionRows rowsFor(long definitionVersionId) {
    LifecycleDefinition version =
        LifecycleDefinition.builder()
            .id(definitionVersionId)
            .scope(DefinitionScope.NEGOTIATION)
            .familyKey(STANDARD_FAMILY)
            .name(STANDARD_FAMILY)
            .version(1)
            .build();
    return DefinitionVersionRows.withoutActions(
        version,
        List.of(stateBuilder(version, "SUBMITTED").initial(true).build()),
        List.of(),
        List.of(),
        List.of());
  }
}
