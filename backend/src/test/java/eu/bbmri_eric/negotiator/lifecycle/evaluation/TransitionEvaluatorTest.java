package eu.bbmri_eric.negotiator.lifecycle.evaluation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import eu.bbmri_eric.negotiator.lifecycle.graph.ActionContext;
import eu.bbmri_eric.negotiator.lifecycle.graph.ActionStep;
import eu.bbmri_eric.negotiator.lifecycle.graph.CompiledGraph;
import eu.bbmri_eric.negotiator.lifecycle.graph.CompiledTransition;
import eu.bbmri_eric.negotiator.lifecycle.graph.EvaluationContext;
import eu.bbmri_eric.negotiator.lifecycle.graph.EvaluationContext.Caller;
import eu.bbmri_eric.negotiator.lifecycle.graph.EvaluationContext.Subject;
import eu.bbmri_eric.negotiator.lifecycle.graph.FailureCategory;
import eu.bbmri_eric.negotiator.lifecycle.graph.GuardStep;
import eu.bbmri_eric.negotiator.lifecycle.graph.GuardVerdict;
import eu.bbmri_eric.negotiator.lifecycle.graph.InvalidGraphException;
import eu.bbmri_eric.negotiator.lifecycle.graph.RequiredAuthority;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
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

  /**
   * A Lifecycle sitting in a State its own pinned Definition Version does not declare: a broken
   * Definition Version Pin, and the fixture for every corruption test below.
   */
  private static EvaluationContext contextInAStateTheVersionDoesNotDeclare(Caller caller) {
    return EvaluationContext.forResource(
        caller,
        Subject.resource(
            "negotiation-1",
            "biobank:1:collection:1",
            "RESOURCE_MADE_AVAILABLE",
            CREATOR,
            Set.of(REPRESENTATIVE)),
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
  @DisplayName("an Event requiring no authority is permitted to any human caller")
  void evaluate_whenTheAuthorityIsNone_permitsAnyHuman() {
    assertThat(evaluator.evaluate(graph(), "ANYONE", contextFor(stranger())))
        .isEqualTo(
            new EvaluationOutcome.Permitted(
                new CompiledTransition("OPEN", "ANYONE", "BY_ANYONE", RequiredAuthority.NONE)));
  }

  @Test
  @DisplayName("IS_ADMIN reads the granted ROLE_ADMIN authority rather than who the caller is")
  void evaluate_whenTheAuthorityIsAdmin_readsTheGrantedAuthorityAndNotTheCallersIdentity() {
    assertThat(evaluator.evaluate(graph(), "ADMIN_ONLY", contextFor(admin())).permitted()).isTrue();
    assertThat(evaluator.evaluate(graph(), "ADMIN_ONLY", contextFor(creator())).permitted())
        .isFalse();
  }

  @Test
  @DisplayName("IS_CREATOR permits the Negotiation's creator and nobody else")
  void evaluate_whenTheAuthorityIsCreator_permitsOnlyTheNegotiationsCreator() {
    assertThat(evaluator.evaluate(graph(), "CREATOR_ONLY", contextFor(creator())).permitted())
        .isTrue();
    assertThat(
            evaluator.evaluate(graph(), "CREATOR_ONLY", contextFor(representative())).permitted())
        .isFalse();
  }

  @Test
  @DisplayName("IS_REPRESENTATIVE permits a representative of that Resource and nobody else")
  void evaluate_whenTheAuthorityIsRepresentative_permitsOnlyARepresentativeOfThatResource() {
    assertThat(evaluator.evaluate(graph(), "REP_ONLY", contextFor(representative())).permitted())
        .isTrue();
    assertThat(evaluator.evaluate(graph(), "REP_ONLY", contextFor(stranger())).permitted())
        .isFalse();
  }

  /** An admin holds no representative rule; nothing partitions callers, and nothing merges them. */
  @Test
  @DisplayName("being an administrator does not make a caller a representative")
  void evaluate_whenTheAuthorityIsRepresentative_isNotSatisfiedByBeingAnAdmin() {
    assertThat(evaluator.evaluate(graph(), "REP_ONLY", contextFor(admin())).permitted()).isFalse();
  }

  @Test
  @DisplayName("a refusal on authority names the authority that was wanted")
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
  @DisplayName("no human caller of any shape satisfies SYSTEM")
  void evaluate_whenTheAuthorityIsSystem_isRefusedForEveryHumanCaller() {
    for (Caller human : List.of(admin(), creator(), representative(), stranger())) {
      assertThat(evaluator.evaluate(graph(), "CONCLUDE", contextFor(human)).permitted()).isFalse();
    }
  }

  @Test
  @DisplayName("the system caller satisfies SYSTEM")
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
  @DisplayName(
      "the system caller satisfies no human authority, so an open Event is not machine-fireable")
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
  @DisplayName("a declared Event that leads nowhere is refused structurally, before any gating")
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
  @DisplayName("an Event the version never declared is refused under its own reason code")
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

  /**
   * Not a refusal at all. A refusal is an answer about a move; this is a statement that the data
   * underneath the question is broken, so it leaves as an exception the cutover slab can catch — a
   * 500 and a log line, never a 403, 422 or 409.
   */
  @Test
  @DisplayName("a Lifecycle in a State the version does not declare is refused as graph corruption")
  void evaluate_whenTheVersionDoesNotDeclareTheCurrentState_throwsRatherThanRefusing() {
    assertThatThrownBy(
            () ->
                evaluator.evaluate(
                    graph(), "ANYONE", contextInAStateTheVersionDoesNotDeclare(admin())))
        .isInstanceOf(InvalidGraphException.class)
        .hasMessageContaining("Definition Version 1")
        .hasMessageContaining("RESOURCE_MADE_AVAILABLE")
        .hasMessageContaining("OPEN");
  }

  /**
   * The pipeline's order, at the very front. Ask whether the Event is declared first and a corrupt
   * pin comes back as {@code UNKNOWN_EVENT} — a message about the caller's input, for a fault in
   * the data. The State question goes first, so this caller's typo never gets the blame.
   */
  @Test
  @DisplayName("a corrupt pin is reported as corruption even when the Event is a typo too")
  void evaluate_whenTheCurrentStateIsUndeclaredAndTheEventUnknown_blamesTheStateAndNotTheEvent() {
    assertThatThrownBy(
            () ->
                evaluator.evaluate(
                    graph(), "TYPO", contextInAStateTheVersionDoesNotDeclare(admin())))
        .isInstanceOf(InvalidGraphException.class)
        .hasMessageContaining("RESOURCE_MADE_AVAILABLE")
        .hasMessageNotContaining("TYPO");
  }

  @Test
  @DisplayName("the listing omits a blocked Event rather than offering it as unavailable")
  void possibleEvents_omitsBlockedEventsRatherThanListingThemAsUnavailable() {
    assertThat(evaluator.possibleEvents(graph(), contextFor(creator())))
        .containsExactlyInAnyOrder("ANYONE", "CREATOR_ONLY");
  }

  @Test
  @DisplayName("a Lifecycle in a terminal State is offered nothing")
  void possibleEvents_whenTheStateIsTerminal_isEmpty() {
    CompiledGraph graph =
        CompiledGraph.builder(1L).initialState("OPEN").terminalState("DONE").build();
    EvaluationContext atDone =
        EvaluationContext.forNegotiation(
            admin(), Subject.negotiation("negotiation-1", "DONE", CREATOR), List.of());

    assertThat(evaluator.possibleEvents(graph, atDone)).isEmpty();
  }

  /**
   * The two failure modes an empty listing would merge, kept apart. A terminal State's listing is
   * empty and means "nothing is available"; a corrupt pin's listing throws. Returning empty for
   * both is the dishonest option user story 64 rules out — a requester would be told there is
   * nothing to do, on a Negotiation whose pinned version cannot describe where it is.
   */
  @Test
  @DisplayName(
      "listing the Possible Events over an undeclared State throws rather than saying none")
  void possibleEvents_whenTheVersionDoesNotDeclareTheCurrentState_throwsRatherThanReturningEmpty() {
    assertThatThrownBy(
            () ->
                evaluator.possibleEvents(graph(), contextInAStateTheVersionDoesNotDeclare(admin())))
        .isInstanceOf(InvalidGraphException.class)
        .hasMessageContaining("Definition Version 1")
        .hasMessageContaining("RESOURCE_MADE_AVAILABLE")
        .hasMessageContaining("OPEN");
  }

  /**
   * ADR 0005's central claim, as a test rather than a sentence. Over every caller shape and every
   * Event the fixture declares: an Event is in the listing exactly when firing it would be
   * permitted. There is one path, so this cannot be made to fail without breaking it.
   */
  @Test
  @DisplayName(
      "the listing offers exactly the Events the gate would permit, for every caller shape")
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
  @DisplayName(
      "an unmet Information Requirement refuses with the stage's category and the step's reason")
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
  @DisplayName(
      "the Information Requirement stage runs on every Transition, with no Wiring row to omit it")
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
  @DisplayName("a caller failing both authority and requirement is told about the authority")
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
  @DisplayName("a caller failing both requirement and Guard is told about the requirement")
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
  @DisplayName("a Guard's refusal is a domain-state conflict that names the refusing Guard")
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
  @DisplayName("a Guard chain stops at the first refusal and the rest never run")
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

  // --- the Action chain, reported and not run --------------------------------------------------

  /**
   * A permitted outcome is a judgement, not a commit. ADR 0001 puts committing a move and running
   * its Actions in the services around the evaluator, so the outcome carries the chain in the order
   * it must run and the evaluator touches none of it.
   *
   * <p>Both halves matter, and the second is the one with teeth: without it, evaluating twenty
   * candidate Events to build a Possible Events listing would set twenty Negotiations' post
   * visibility.
   */
  @Test
  @DisplayName("a permitted outcome reports its ordered Action chain and runs none of it")
  void evaluate_whenPermitted_reportsTheOrderedActionChainAndRunsNoneOfIt() {
    List<String> ran = new java.util.ArrayList<>();
    CompiledGraph withActions =
        CompiledGraph.builder(1L)
            .initialState("OPEN")
            .state("MOVED")
            .transition(
                new CompiledTransition(
                    "OPEN",
                    "GO",
                    "MOVED",
                    RequiredAuthority.NONE,
                    List.of(),
                    List.of(
                        recordingAction("FIRST", ran),
                        recordingAction("SECOND", ran),
                        recordingAction("THIRD", ran))))
            .build();

    EvaluationOutcome outcome = evaluator.evaluate(withActions, "GO", contextFor(stranger()));

    assertThat(outcome)
        .asInstanceOf(
            org.assertj.core.api.InstanceOfAssertFactories.type(EvaluationOutcome.Permitted.class))
        .satisfies(
            permitted ->
                assertThat(permitted.actions())
                    .extracting(ActionStep::typeKey)
                    .containsExactly("FIRST", "SECOND", "THIRD"));
    assertThat(ran).isEmpty();
  }

  /**
   * The same, over the listing rather than one Event. This is the path that would multiply an
   * effect by the size of the fan the caller is offered.
   */
  @Test
  @DisplayName("listing the Possible Events runs no Action either")
  void possibleEvents_runsNoAction() {
    List<String> ran = new java.util.ArrayList<>();
    CompiledGraph withActions =
        CompiledGraph.builder(1L)
            .initialState("OPEN")
            .state("MOVED")
            .state("ALSO_MOVED")
            .transition(
                new CompiledTransition(
                    "OPEN",
                    "GO",
                    "MOVED",
                    RequiredAuthority.NONE,
                    List.of(),
                    List.of(recordingAction("ON_GO", ran))))
            .transition(
                new CompiledTransition(
                    "OPEN",
                    "ALSO_GO",
                    "ALSO_MOVED",
                    RequiredAuthority.NONE,
                    List.of(),
                    List.of(recordingAction("ON_ALSO_GO", ran))))
            .build();

    assertThat(evaluator.possibleEvents(withActions, contextFor(stranger())))
        .containsExactlyInAnyOrder("GO", "ALSO_GO");
    assertThat(ran).isEmpty();
  }

  private static ActionStep recordingAction(String typeKey, List<String> ran) {
    return new ActionStep() {
      @Override
      public String typeKey() {
        return typeKey;
      }

      @Override
      public void run(ActionContext context) {
        ran.add(typeKey);
      }
    };
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
