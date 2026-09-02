package eu.bbmri_eric.negotiator.lifecycle.definition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import eu.bbmri_eric.negotiator.lifecycle.graph.CompiledGraph;
import eu.bbmri_eric.negotiator.lifecycle.graph.CompiledTransition;
import eu.bbmri_eric.negotiator.lifecycle.graph.EvaluationContext;
import eu.bbmri_eric.negotiator.lifecycle.graph.GuardCatalogue;
import eu.bbmri_eric.negotiator.lifecycle.graph.GuardStep;
import eu.bbmri_eric.negotiator.lifecycle.graph.GuardVerdict;
import eu.bbmri_eric.negotiator.lifecycle.graph.RequiredAuthority;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Definition Compilation, with no database and no Spring. The compiler takes {@link
 * DefinitionVersionRows} instead of six repositories so that this test can build the rows in
 * memory, without persisting them or giving them row ids.
 *
 * <p>The Guard catalogue is a local fake, because the test covers the fold (which rows become which
 * chain, in which order), not what a strategy does. The fake records the keys it was asked for, so
 * the test sees the order of the effective chain without running a Guard.
 *
 * <p>The builders stay here instead of in {@link DefinitionFixtures}. The repository tests hold the
 * Required Authority and the params column fixed while varying the table under test, and this test
 * does the opposite, so a shared builder would blur each caller's subject.
 */
class DefinitionCompilerTest {

  private static final long VERSION_ID = 42L;

  private final RecordingCatalogue catalogue = new RecordingCatalogue();
  private final DefinitionCompiler compiler = new DefinitionCompiler(catalogue);

  private final LifecycleDefinition version =
      LifecycleDefinition.builder()
          .id(VERSION_ID)
          .scope(DefinitionScope.RESOURCE)
          .familyKey("standard-resource-flow")
          .name("Standard flow")
          .version(1)
          .active(true)
          .build();

  private final State submitted =
      State.builder()
          .lifecycleDefinition(version)
          .name("SUBMITTED")
          .label("Submitted")
          .initial(true)
          .build();
  private final State available =
      State.builder()
          .lifecycleDefinition(version)
          .name("RESOURCE_MADE_AVAILABLE")
          .label("Made available")
          .terminal(true)
          .build();
  private final State legacy =
      State.builder()
          .lifecycleDefinition(version)
          .name("RETURNED_FOR_RESUBMISSION")
          .label("Returned")
          .build();

  private final Event deliver = eventNamed("DELIVER");
  private final Event override = eventNamed("OVERRIDE");

  private final Transition deliverTransition =
      transition(submitted, deliver, available, RequiredAuthority.IS_REPRESENTATIVE);

  private Event eventNamed(String name) {
    return Event.builder().lifecycleDefinition(version).name(name).build();
  }

  private Transition transition(
      State from, Event event, State to, RequiredAuthority requiredAuthority) {
    return Transition.builder()
        .lifecycleDefinition(version)
        .fromState(from)
        .event(event)
        .toState(to)
        .requiredAuthority(requiredAuthority)
        .build();
  }

  private GuardWiring definitionWide(String typeKey, int sortOrder, String params) {
    return GuardWiring.builder()
        .lifecycleDefinition(version)
        .transition(null)
        .typeKey(typeKey)
        .params(params)
        .sortOrder(sortOrder)
        .build();
  }

  private GuardWiring onTransition(
      Transition transition, String typeKey, int sortOrder, String params) {
    return GuardWiring.builder()
        .lifecycleDefinition(version)
        .transition(transition)
        .typeKey(typeKey)
        .params(params)
        .sortOrder(sortOrder)
        .build();
  }

  private DefinitionVersionRows rows(List<Transition> transitions, List<GuardWiring> wirings) {
    return new DefinitionVersionRows(
        version,
        List.of(submitted, available, legacy),
        List.of(deliver, override),
        transitions,
        wirings);
  }

  @Test
  void compile_takesTheDefinitionVersionsRowIdAsTheGraphsIdentity() {
    CompiledGraph graph = compiler.compile(rows(List.of(deliverTransition), List.of()));

    assertThat(graph.definitionVersionId()).isEqualTo(VERSION_ID);
  }

  @Test
  void compile_carriesTheStateFlagsAcrossAsTheyAreDeclared() {
    CompiledGraph graph = compiler.compile(rows(List.of(deliverTransition), List.of()));

    assertThat(graph.initialState()).isEqualTo("SUBMITTED");
    assertThat(graph.isTerminal("RESOURCE_MADE_AVAILABLE")).isTrue();
    assertThat(graph.isTerminal("SUBMITTED")).isFalse();
  }

  /** A Legacy State must survive compilation to keep old data valid. */
  @Test
  void compile_keepsAStateNoTransitionTouches() {
    CompiledGraph graph = compiler.compile(rows(List.of(deliverTransition), List.of()));

    assertThat(graph.declaresState("RETURNED_FOR_RESUBMISSION")).isTrue();
    assertThat(graph.transitionsFrom("RETURNED_FOR_RESUBMISSION")).isEmpty();
  }

  @Test
  void compile_keepsAnEventNoTransitionUses() {
    CompiledGraph graph = compiler.compile(rows(List.of(deliverTransition), List.of()));

    assertThat(graph.declaresEvent("OVERRIDE")).isTrue();
    assertThat(graph.transition("SUBMITTED", "OVERRIDE")).isEmpty();
  }

  @Test
  void compile_carriesTheRequiredAuthorityOntoTheEdge() {
    CompiledGraph graph = compiler.compile(rows(List.of(deliverTransition), List.of()));

    assertThat(graph.transition("SUBMITTED", "DELIVER"))
        .get()
        .extracting(CompiledTransition::requiredAuthority)
        .isEqualTo(RequiredAuthority.IS_REPRESENTATIVE);
  }

  /**
   * The compiler folds the two scopes so the Transition Evaluator gets one chain and never learns
   * that Guards have two scopes.
   */
  @Test
  void compile_putsDefinitionWideGuardsAheadOfTheTransitionsOwn() {
    CompiledGraph graph =
        compiler.compile(
            rows(
                List.of(deliverTransition),
                List.of(
                    onTransition(deliverTransition, "TRANSITION_SECOND", 2, null),
                    definitionWide("DEFINITION_FIRST", 1, null),
                    onTransition(deliverTransition, "TRANSITION_FIRST", 1, null),
                    definitionWide("DEFINITION_SECOND", 2, null))));

    assertThat(chainOf(graph, "SUBMITTED", "DELIVER"))
        .containsExactly(
            "DEFINITION_FIRST", "DEFINITION_SECOND", "TRANSITION_FIRST", "TRANSITION_SECOND");
  }

  @Test
  void compile_ordersEachScopeByItsOwnSortOrderRatherThanByRowOrder() {
    CompiledGraph graph =
        compiler.compile(
            rows(
                List.of(deliverTransition),
                List.of(
                    definitionWide("THIRD", 30, null),
                    definitionWide("FIRST", 10, null),
                    definitionWide("SECOND", 20, null))));

    assertThat(chainOf(graph, "SUBMITTED", "DELIVER")).containsExactly("FIRST", "SECOND", "THIRD");
  }

  /**
   * Two partial unique indexes give the two scopes independent {@code sort_order} sequences. The
   * schema accepts the same number on both sides, and the fold must not interleave them.
   */
  @Test
  void compile_whenBothScopesUseTheSameSortOrder_stillPutsTheDefinitionWideOneFirst() {
    CompiledGraph graph =
        compiler.compile(
            rows(
                List.of(deliverTransition),
                List.of(
                    onTransition(deliverTransition, "TRANSITION_SCOPED", 1, null),
                    definitionWide("DEFINITION_WIDE", 1, null))));

    assertThat(chainOf(graph, "SUBMITTED", "DELIVER"))
        .containsExactly("DEFINITION_WIDE", "TRANSITION_SCOPED");
  }

  /** There is no uniqueness on {@code type_key}: one definition may wire a key twice. */
  @Test
  void compile_whenOneTypeKeyIsWiredTwiceAtDifferentSortOrders_keepsBoth() {
    CompiledGraph graph =
        compiler.compile(
            rows(
                List.of(deliverTransition),
                List.of(definitionWide("TWICE", 1, null), definitionWide("TWICE", 2, null))));

    assertThat(chainOf(graph, "SUBMITTED", "DELIVER")).containsExactly("TWICE", "TWICE");
  }

  /**
   * A definition-wide Guard applies to every Transition of the version, so a Transition that a later
   * version adds gets it without anyone wiring it again.
   */
  @Test
  void compile_appliesADefinitionWideGuardToEveryTransition() {
    Transition second = transition(submitted, override, legacy, RequiredAuthority.IS_ADMIN);
    CompiledGraph graph =
        compiler.compile(
            rows(
                List.of(deliverTransition, second),
                List.of(definitionWide("NEGOTIATION_APPROVED", 1, null))));

    assertThat(chainOf(graph, "SUBMITTED", "DELIVER")).containsExactly("NEGOTIATION_APPROVED");
    assertThat(chainOf(graph, "SUBMITTED", "OVERRIDE")).containsExactly("NEGOTIATION_APPROVED");
  }

  @Test
  void compile_keepsATransitionScopedGuardOffEveryOtherTransition() {
    Transition second = transition(submitted, override, legacy, RequiredAuthority.IS_ADMIN);
    CompiledGraph graph =
        compiler.compile(
            rows(
                List.of(deliverTransition, second),
                List.of(onTransition(deliverTransition, "ONLY_HERE", 1, null))));

    assertThat(chainOf(graph, "SUBMITTED", "DELIVER")).containsExactly("ONLY_HERE");
    assertThat(chainOf(graph, "SUBMITTED", "OVERRIDE")).isEmpty();
  }

  @Test
  void compile_handsEachWiringsRawParamsToTheCatalogueOnce() {
    compiler.compile(
        rows(
            List.of(deliverTransition),
            List.of(definitionWide("SET_POST_VISIBILITY", 1, "{\"scope\":\"BOTH\"}"))));

    assertThat(catalogue.bound)
        .containsExactly(new Bound("SET_POST_VISIBILITY", "{\"scope\":\"BOTH\"}"));
  }

  /**
   * The compiler binds the row once and shares the step across Transitions. A step holds only
   * configuration, so the same instance is safe on every edge.
   */
  @Test
  void compile_bindsADefinitionWideWiringOnceHoweverManyTransitionsCarryIt() {
    Transition second = transition(submitted, override, legacy, RequiredAuthority.IS_ADMIN);

    compiler.compile(
        rows(
            List.of(deliverTransition, second),
            List.of(definitionWide("NEGOTIATION_APPROVED", 1, null))));

    assertThat(catalogue.bound).hasSize(1);
  }

  @Test
  void compile_whenTheCatalogueRefusesAKey_failsTheCompileRatherThanTheFiring() {
    assertThatThrownBy(
            () ->
                compiler.compile(
                    rows(List.of(deliverTransition), List.of(definitionWide("NO_SUCH", 1, null)))))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("NO_SUCH");
  }

  @Test
  void compile_whenARowBelongsToAnotherDefinitionVersion_isRefused() {
    LifecycleDefinition other =
        LifecycleDefinition.builder()
            .id(99L)
            .scope(DefinitionScope.RESOURCE)
            .familyKey("other")
            .name("Other")
            .version(1)
            .build();
    State foreign =
        State.builder().lifecycleDefinition(other).name("ELSEWHERE").label("Elsewhere").build();

    DefinitionVersionRows mixed =
        new DefinitionVersionRows(
            version,
            List.of(submitted, foreign),
            List.of(deliver),
            List.of(deliverTransition),
            List.of());

    assertThatThrownBy(() -> compiler.compile(mixed))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("different Definition Version")
        .hasMessageContaining("ELSEWHERE");
  }

  private static List<String> chainOf(CompiledGraph graph, String fromState, String event) {
    return graph.transition(fromState, event).orElseThrow().guards().stream()
        .map(GuardStep::typeKey)
        .toList();
  }

  private record Bound(String typeKey, String paramsJson) {}

  /**
   * A catalogue that binds anything whose key is not {@code NO_SUCH}, and remembers what it was
   * asked. Standing in for the registry keeps this test about the fold rather than about Jackson.
   */
  private static final class RecordingCatalogue implements GuardCatalogue {

    private final List<Bound> bound = new ArrayList<>();

    @Override
    public GuardStep bind(String typeKey, String paramsJson) {
      if ("NO_SUCH".equals(typeKey)) {
        throw new IllegalArgumentException("No Guard strategy declares the type key 'NO_SUCH'.");
      }
      bound.add(new Bound(typeKey, paramsJson));
      return new GuardStep() {
        @Override
        public String typeKey() {
          return typeKey;
        }

        @Override
        public GuardVerdict check(EvaluationContext context) {
          return GuardVerdict.pass();
        }
      };
    }
  }
}
