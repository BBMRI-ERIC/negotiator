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
 * How often the package loads and compiles, with the loader mocked so every load is counted.
 *
 * <p>Two resolutions of one version return the same instance whether it compiled once or twice,
 * so only a count shows the cache works. {@link CompiledGraphCacheTest} tests the cache alone; this
 * test catches production code that builds a fresh cache per call.
 */
class LifecycleDefinitionsImplTest {

  private static final long A_VERSION = 11L;
  private static final long ANOTHER_VERSION = 12L;

  /** Three Resources across two versions, where a naive lookup would load once per Resource. */
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
   * Three Resources across two versions cost two loads, not three. The map has one entry per
   * distinct version, which the caller looks each Resource's pin up in. Most Resources share a
   * version, so the repeated id must cost nothing.
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

  /** A version already resolved is not loaded again for a batch. */
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
   * Both pin columns are nullable, so a caller collecting pins can hold a null. It must get a clear
   * refusal, not an unboxing failure. The null is second in the list to show the batch is checked
   * before anything loads.
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
   * A definition repaired after a bad compile is served without a restart. The second call must
   * reach the loader and not return a remembered failure.
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
   * Definition Resolution is delegated to the resolver and answers with a row id. Nothing else
   * catches a {@code resolveForResource} that answered the Negotiation question.
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
   * The smallest graph that compiles: one Definition Version and its initial State. These tests
   * care only about which version a graph belongs to.
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
