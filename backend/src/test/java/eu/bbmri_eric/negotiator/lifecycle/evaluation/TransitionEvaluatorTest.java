package eu.bbmri_eric.negotiator.lifecycle.evaluation;

import static org.assertj.core.api.Assertions.assertThat;

import eu.bbmri_eric.negotiator.lifecycle.graph.CompiledGraph;
import eu.bbmri_eric.negotiator.lifecycle.graph.CompiledTransition;
import eu.bbmri_eric.negotiator.lifecycle.graph.EvaluationContext;
import eu.bbmri_eric.negotiator.lifecycle.graph.EvaluationContext.Caller;
import eu.bbmri_eric.negotiator.lifecycle.graph.EvaluationContext.Subject;
import eu.bbmri_eric.negotiator.lifecycle.graph.FailureCategory;
import eu.bbmri_eric.negotiator.lifecycle.graph.GuardStep;
import eu.bbmri_eric.negotiator.lifecycle.graph.GuardVerdict;
import eu.bbmri_eric.negotiator.lifecycle.graph.RequiredAuthority;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * The whole Evaluation Pipeline: Required Authority, the Information Requirement stage, then the
 * Guard chain. No Spring and no database — the evaluator takes a graph and a context, and the only
 * thing stood in for is the one port, because the one port is the only thing that would read data.
 *
 * <p>The graph puts all five Required Authority values on Transitions leaving one State, so a
 * single context can be asked about every rule at once and {@link
 * #possibleEvents_offersExactlyWhatEvaluateWouldPermit} can compare the two entry points over the
 * whole fan-out rather than one edge at a time.
 *
 * <p>Three tests are about the pipeline's <em>order</em> rather than about any one stage, and they
 * are the ones that keep ADR 0005's categories monotonic: a caller failing two stages is told about
 * the earlier one, in both of the two places that can happen.
 */
class TransitionEvaluatorTest {

  private static final long CREATOR = 101L;
  private static final long REPRESENTATIVE = 108L;
  private static final long STRANGER = 999L;

  private final StubRequirements requirements = new StubRequirements();
  private final TransitionEvaluator evaluator = new TransitionEvaluator(requirements);

  private static CompiledGraph graph() {
    return CompiledGraph.builder(1L)
        .initialState("OPEN")
        .state("BY_ANYONE")
        .state("BY_ADMIN")
        .state("BY_CREATOR")
        .state("BY_REPRESENTATIVE")
        .state("BY_SYSTEM")
        .transition("OPEN", "ANYONE", "BY_ANYONE", RequiredAuthority.NONE)
        .transition("OPEN", "ADMIN_ONLY", "BY_ADMIN", RequiredAuthority.IS_ADMIN)
        .transition("OPEN", "CREATOR_ONLY", "BY_CREATOR", RequiredAuthority.IS_CREATOR)
        .transition("OPEN", "REP_ONLY", "BY_REPRESENTATIVE", RequiredAuthority.IS_REPRESENTATIVE)
        .transition("OPEN", "CONCLUDE", "BY_SYSTEM", RequiredAuthority.SYSTEM)
        .eventWithoutTransition("OVERRIDE")
        .build();
  }

  private static EvaluationContext contextFor(Caller caller) {
    return EvaluationContext.forResource(
        caller,
        Subject.resource(
            "negotiation-1", "biobank:1:collection:1", "OPEN", CREATOR, Set.of(REPRESENTATIVE)),
        "IN_PROGRESS");
  }

  private static Caller admin() {
    return Caller.person(STRANGER, Set.of("ROLE_ADMIN"));
  }

  private static Caller creator() {
    return Caller.person(CREATOR, Set.of());
  }

  private static Caller representative() {
    return Caller.person(REPRESENTATIVE, Set.of());
  }

  private static Caller stranger() {
    return Caller.person(STRANGER, Set.of());
  }

  @Test
  void evaluate_whenTheAuthorityIsNone_permitsAnyHuman() {
    assertThat(evaluator.evaluate(graph(), "ANYONE", contextFor(stranger())))
        .isEqualTo(
            new EvaluationOutcome.Permitted(
                new CompiledTransition("OPEN", "ANYONE", "BY_ANYONE", RequiredAuthority.NONE)));
  }

  @Test
  void evaluate_whenTheAuthorityIsAdmin_readsTheGrantedAuthorityAndNotTheCallersIdentity() {
    assertThat(evaluator.evaluate(graph(), "ADMIN_ONLY", contextFor(admin())).permitted()).isTrue();
    assertThat(evaluator.evaluate(graph(), "ADMIN_ONLY", contextFor(creator())).permitted())
        .isFalse();
  }

  @Test
  void evaluate_whenTheAuthorityIsCreator_permitsOnlyTheNegotiationsCreator() {
    assertThat(evaluator.evaluate(graph(), "CREATOR_ONLY", contextFor(creator())).permitted())
        .isTrue();
    assertThat(
            evaluator.evaluate(graph(), "CREATOR_ONLY", contextFor(representative())).permitted())
        .isFalse();
  }

  @Test
  void evaluate_whenTheAuthorityIsRepresentative_permitsOnlyARepresentativeOfThatResource() {
    assertThat(evaluator.evaluate(graph(), "REP_ONLY", contextFor(representative())).permitted())
        .isTrue();
    assertThat(evaluator.evaluate(graph(), "REP_ONLY", contextFor(stranger())).permitted())
        .isFalse();
  }

  /** An admin holds no representative rule; nothing partitions callers, and nothing merges them. */
  @Test
  void evaluate_whenTheAuthorityIsRepresentative_isNotSatisfiedByBeingAnAdmin() {
    assertThat(evaluator.evaluate(graph(), "REP_ONLY", contextFor(admin())).permitted()).isFalse();
  }

  @Test
  void evaluate_whenRefusedOnAuthority_saysWhichAuthorityWasWanted() {
    EvaluationOutcome outcome = evaluator.evaluate(graph(), "ADMIN_ONLY", contextFor(stranger()));

    assertThat(outcome)
        .isInstanceOf(EvaluationOutcome.Refused.class)
        .asInstanceOf(
            org.assertj.core.api.InstanceOfAssertFactories.type(EvaluationOutcome.Refused.class))
        .satisfies(
            refused -> {
              assertThat(refused.category()).isEqualTo(FailureCategory.AUTHORIZATION);
              assertThat(refused.reasonCode()).isEqualTo("REQUIRED_AUTHORITY_NOT_HELD");
              assertThat(refused.details()).containsEntry("requiredAuthority", "IS_ADMIN");
            });
  }

  /**
   * ADR 0007: an Event is machine-fired if and only if its authority is {@code SYSTEM}. Walked over
   * every human caller shape the fixture has, so "no human at all" is a claim about the rule rather
   * than about one caller.
   */
  @Test
  void evaluate_whenTheAuthorityIsSystem_isRefusedForEveryHumanCaller() {
    for (Caller human : List.of(admin(), creator(), representative(), stranger())) {
      assertThat(evaluator.evaluate(graph(), "CONCLUDE", contextFor(human)).permitted()).isFalse();
    }
  }

  @Test
  void evaluate_whenTheAuthorityIsSystem_permitsTheSystemCaller() {
    assertThat(evaluator.evaluate(graph(), "CONCLUDE", contextFor(Caller.system())).permitted())
        .isTrue();
  }

  /**
   * The other direction, which is the less obvious half: reading {@code NONE} as "including the
   * system" would make every open Event machine-fireable, and that overload is exactly what {@code
   * SYSTEM} exists to prevent.
   */
  @ParameterizedTest
  @EnumSource(
      value = RequiredAuthority.class,
      names = {"NONE", "IS_ADMIN", "IS_CREATOR", "IS_REPRESENTATIVE"})
  void evaluate_whenTheCallerIsTheSystem_satisfiesNoHumanAuthority(RequiredAuthority authority) {
    CompiledGraph graph =
        CompiledGraph.builder(1L)
            .initialState("OPEN")
            .state("MOVED")
            .transition("OPEN", "GO", "MOVED", authority)
            .build();

    assertThat(evaluator.evaluate(graph, "GO", contextFor(Caller.system())).permitted()).isFalse();
  }

  /**
   * The Override Event's shape: a real, declared Event that leads nowhere. Refused structurally,
   * before any gating, and told apart from a name the version has never heard of.
   */
  @Test
  void evaluate_whenTheEventIsDeclaredButCarriesNoTransition_refusesBeforeAnyGating() {
    EvaluationOutcome outcome = evaluator.evaluate(graph(), "OVERRIDE", contextFor(admin()));

    assertThat(outcome)
        .asInstanceOf(
            org.assertj.core.api.InstanceOfAssertFactories.type(EvaluationOutcome.Refused.class))
        .satisfies(
            refused -> {
              assertThat(refused.category()).isEqualTo(FailureCategory.NO_TRANSITION);
              assertThat(refused.reasonCode()).isEqualTo("NO_TRANSITION_FOR_EVENT");
              assertThat(refused.details())
                  .containsEntry("event", "OVERRIDE")
                  .containsEntry("fromState", "OPEN");
            });
  }

  @Test
  void evaluate_whenTheVersionDeclaresNoSuchEventAtAll_saysSoDistinctly() {
    EvaluationOutcome outcome = evaluator.evaluate(graph(), "TYPO", contextFor(admin()));

    assertThat(outcome)
        .asInstanceOf(
            org.assertj.core.api.InstanceOfAssertFactories.type(EvaluationOutcome.Refused.class))
        .satisfies(
            refused -> {
              assertThat(refused.category()).isEqualTo(FailureCategory.NO_TRANSITION);
              assertThat(refused.reasonCode()).isEqualTo("UNKNOWN_EVENT");
              assertThat(refused.details()).containsEntry("event", "TYPO");
            });
  }

  @Test
  void possibleEvents_omitsBlockedEventsRatherThanListingThemAsUnavailable() {
    assertThat(evaluator.possibleEvents(graph(), contextFor(creator())))
        .containsExactlyInAnyOrder("ANYONE", "CREATOR_ONLY");
  }

  @Test
  void possibleEvents_whenTheStateIsTerminal_isEmpty() {
    CompiledGraph graph =
        CompiledGraph.builder(1L).initialState("OPEN").terminalState("DONE").build();
    EvaluationContext atDone =
        EvaluationContext.forNegotiation(
            admin(), Subject.negotiation("negotiation-1", "DONE", CREATOR), List.of());

    assertThat(evaluator.possibleEvents(graph, atDone)).isEmpty();
  }

  /**
   * ADR 0005's central claim, as a test rather than a sentence. Over every caller shape and every
   * Event the fixture declares: an Event is in the listing exactly when firing it would be
   * permitted. There is one path, so this cannot be made to fail without breaking it.
   */
  @Test
  void possibleEvents_offersExactlyWhatEvaluateWouldPermit() {
    CompiledGraph graph = graph();
    List<String> everyEvent =
        List.of("ANYONE", "ADMIN_ONLY", "CREATOR_ONLY", "REP_ONLY", "CONCLUDE", "OVERRIDE", "TYPO");

    for (Caller caller :
        List.of(admin(), creator(), representative(), stranger(), Caller.system())) {
      EvaluationContext context = contextFor(caller);
      Set<String> listed = evaluator.possibleEvents(graph, context);

      for (String event : everyEvent) {
        assertThat(listed.contains(event))
            .as("caller %s, event %s", caller, event)
            .isEqualTo(evaluator.evaluate(graph, event, context).permitted());
      }
    }
  }

  // --- the Information Requirement stage, and the pipeline's order ---------------------------

  @Test
  void evaluate_whenARequirementIsUnmet_refusesWithTheStagesOwnCategoryAndTheStepsReason() {
    requirements.blocked.put(
        "ANYONE", GuardVerdict.fail("REQUIREMENT_NOT_MET", Map.of("missingForms", List.of("7"))));

    EvaluationOutcome outcome = evaluator.evaluate(graph(), "ANYONE", contextFor(stranger()));

    assertThat(outcome)
        .asInstanceOf(
            org.assertj.core.api.InstanceOfAssertFactories.type(EvaluationOutcome.Refused.class))
        .satisfies(
            refused -> {
              assertThat(refused.category()).isEqualTo(FailureCategory.UNMET_REQUIREMENT);
              assertThat(refused.reasonCode()).isEqualTo("REQUIREMENT_NOT_MET");
              assertThat(refused.details()).containsEntry("missingForms", List.of("7"));
            });
  }

  /**
   * The whole reason the stage is built in. There is no Wiring row that could omit it, so it runs
   * on every Transition of every version — including one added later by an admin who has never
   * heard of it.
   */
  @Test
  void evaluate_runsTheRequirementStageOnEveryTransitionWithoutAnyWiring() {
    requirements.blockEverything();

    for (String event : List.of("ANYONE", "ADMIN_ONLY", "CREATOR_ONLY", "REP_ONLY")) {
      assertThat(evaluator.evaluate(graph(), event, contextFor(admin())).permitted())
          .as("event %s", event)
          .isFalse();
    }
  }

  /**
   * ADR 0005's order, asserted where it is observable: authority is checked first, so a caller who
   * holds neither the authority nor the requirement is told about the authority. This is what "the
   * category a caller sees never flip-flops" means, and it is also what stops an unauthorised
   * caller learning that a Requirement exists at all — which is how the check behaves today, and
   * was pinned as a leak.
   */
  @Test
  void evaluate_whenBothAuthorityAndRequirementFail_reportsTheAuthorityOne() {
    requirements.blockEverything();

    EvaluationOutcome outcome = evaluator.evaluate(graph(), "ADMIN_ONLY", contextFor(stranger()));

    assertThat(outcome)
        .asInstanceOf(
            org.assertj.core.api.InstanceOfAssertFactories.type(EvaluationOutcome.Refused.class))
        .extracting(EvaluationOutcome.Refused::category)
        .isEqualTo(FailureCategory.AUTHORIZATION);
  }

  /** And the second half of the order: a Requirement outranks a Guard. */
  @Test
  void evaluate_whenBothRequirementAndGuardFail_reportsTheRequirementOne() {
    requirements.blockEverything();
    CompiledGraph guarded =
        CompiledGraph.builder(1L)
            .initialState("OPEN")
            .state("MOVED")
            .transition(
                new CompiledTransition(
                    "OPEN",
                    "GO",
                    "MOVED",
                    RequiredAuthority.NONE,
                    List.of(alwaysFailing("ALWAYS_NO"))))
            .build();

    EvaluationOutcome outcome = evaluator.evaluate(guarded, "GO", contextFor(stranger()));

    assertThat(outcome)
        .asInstanceOf(
            org.assertj.core.api.InstanceOfAssertFactories.type(EvaluationOutcome.Refused.class))
        .extracting(EvaluationOutcome.Refused::category)
        .isEqualTo(FailureCategory.UNMET_REQUIREMENT);
  }

  // --- the Guard stage ------------------------------------------------------------------------

  @Test
  void evaluate_whenAGuardRefuses_reportsADomainStateConflictNamingTheGuard() {
    CompiledGraph guarded =
        CompiledGraph.builder(1L)
            .initialState("OPEN")
            .state("MOVED")
            .transition(
                new CompiledTransition(
                    "OPEN",
                    "GO",
                    "MOVED",
                    RequiredAuthority.NONE,
                    List.of(alwaysFailing("TERMINAL_AGGREGATION"))))
            .build();

    EvaluationOutcome outcome = evaluator.evaluate(guarded, "GO", contextFor(stranger()));

    assertThat(outcome)
        .asInstanceOf(
            org.assertj.core.api.InstanceOfAssertFactories.type(EvaluationOutcome.Refused.class))
        .satisfies(
            refused -> {
              assertThat(refused.category()).isEqualTo(FailureCategory.DOMAIN_STATE_CONFLICT);
              assertThat(refused.reasonCode()).isEqualTo("NOPE");
              assertThat(refused.details()).containsEntry("guard", "TERMINAL_AGGREGATION");
            });
  }

  /** Short-circuiting is observable: a chain stops at the first refusal and the rest never run. */
  @Test
  void evaluate_whenAGuardRefuses_doesNotRunTheRestOfTheChain() {
    List<String> ran = new java.util.ArrayList<>();
    CompiledGraph guarded =
        CompiledGraph.builder(1L)
            .initialState("OPEN")
            .state("MOVED")
            .transition(
                new CompiledTransition(
                    "OPEN",
                    "GO",
                    "MOVED",
                    RequiredAuthority.NONE,
                    List.of(
                        recording("FIRST", ran, GuardVerdict.pass()),
                        recording("SECOND", ran, GuardVerdict.fail("NOPE")),
                        recording("THIRD", ran, GuardVerdict.pass()))))
            .build();

    evaluator.evaluate(guarded, "GO", contextFor(stranger()));

    assertThat(ran).containsExactly("FIRST", "SECOND");
  }

  private static GuardStep alwaysFailing(String typeKey) {
    return recording(typeKey, new java.util.ArrayList<>(), GuardVerdict.fail("NOPE"));
  }

  private static GuardStep recording(String typeKey, List<String> ran, GuardVerdict verdict) {
    return new GuardStep() {
      @Override
      public String typeKey() {
        return typeKey;
      }

      @Override
      public GuardVerdict check(EvaluationContext context) {
        ran.add(typeKey);
        return verdict;
      }
    };
  }

  /**
   * The port's test double. Satisfied unless a test says otherwise, because that is the ordinary
   * case: an Event with no Information Requirement is most Events.
   */
  private static final class StubRequirements implements InformationRequirementSatisfaction {

    private final Map<String, GuardVerdict> blocked = new java.util.HashMap<>();
    private GuardVerdict everything;

    void blockEverything() {
      everything = GuardVerdict.fail("REQUIREMENT_NOT_MET");
    }

    @Override
    public GuardVerdict check(String event, EvaluationContext context) {
      if (everything != null) {
        return everything;
      }
      return blocked.getOrDefault(event, GuardVerdict.pass());
    }
  }
}
