package eu.bbmri_eric.negotiator.lifecycle.definition;

import static eu.bbmri_eric.negotiator.lifecycle.definition.DefinitionFixtures.OTHER_FAMILY;
import static eu.bbmri_eric.negotiator.lifecycle.definition.DefinitionFixtures.STANDARD_FAMILY;
import static eu.bbmri_eric.negotiator.lifecycle.definition.DefinitionFixtures.definitionIn;
import static eu.bbmri_eric.negotiator.lifecycle.definition.DefinitionFixtures.eventIn;
import static eu.bbmri_eric.negotiator.lifecycle.definition.DefinitionFixtures.stateBuilder;
import static eu.bbmri_eric.negotiator.lifecycle.definition.DefinitionFixtures.stateIn;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import eu.bbmri_eric.negotiator.lifecycle.graph.ActionStep;
import eu.bbmri_eric.negotiator.lifecycle.graph.CompiledGraph;
import eu.bbmri_eric.negotiator.lifecycle.graph.GuardStep;
import eu.bbmri_eric.negotiator.lifecycle.graph.InvalidGraphException;
import eu.bbmri_eric.negotiator.lifecycle.graph.RequiredAuthority;
import eu.bbmri_eric.negotiator.util.RepositoryTest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.util.Set;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Which rows {@link DefinitionVersionLoader} reads for one Definition Version id, against a real
 * PostgreSQL.
 *
 * <p>Each test writes two Definition Versions and asks for one. A loader with a wrong {@code WHERE}
 * clause would mix rows of both versions, and a single-version fixture would not catch it. The
 * second version has a row in all five tables for the same reason, including the table reached
 * through a join instead of a definition column.
 *
 * <p>This class cannot test the loader's transaction. {@code @DataJpaTest} wraps each test in one,
 * so the persistence-context identity that {@link DefinitionCompiler} depends on holds here even
 * without the annotation on {@link DefinitionVersionLoader#load}. {@code
 * CompiledGraphResolutionIntegrationTest} covers the transaction by committing.
 */
@RepositoryTest
@Import(DefinitionVersionLoader.class)
class DefinitionVersionLoaderTest {

  /** One query per table. */
  private static final int QUERIES_PER_LOAD = 6;

  private static final String APPROVED_GUARD = "NEGOTIATION_APPROVED";
  private static final String TERMINAL_GUARD = "TERMINAL_AGGREGATION";
  private static final String VISIBILITY_ACTION = "SET_POST_VISIBILITY";
  private static final String VISIBILITY_PARAMS = "{\"scope\":\"BOTH\",\"enabled\":false}";

  @Autowired DefinitionVersionLoader loader;
  @Autowired LifecycleDefinitionRepository definitions;
  @Autowired StateRepository states;
  @Autowired EventRepository events;
  @Autowired TransitionRepository transitions;
  @Autowired GuardWiringRepository guardWirings;
  @Autowired ActionWiringRepository actionWirings;
  @Autowired EntityManager entityManager;
  @Autowired EntityManagerFactory entityManagerFactory;

  private long subjectId;

  @BeforeEach
  void writeTwoDefinitionVersions() {
    subjectId = writeSubject();
    writeTheOtherVersion();
    entityManager.flush();
  }

  @Test
  @DisplayName("every row of the version comes back, and nothing of the version beside it")
  void load_readsEveryRowOfTheVersionAndNoOther() {
    DefinitionVersionRows rows = loader.load(subjectId);

    assertThat(rows.definition().getId()).isEqualTo(subjectId);
    assertThat(rows.states()).extracting(State::getName).containsExactly("SUBMITTED", "CONCLUDED");
    assertThat(rows.events()).extracting(Event::getName).containsExactly("CONCLUDE", "OVERRIDE");
    assertThat(rows.transitions())
        .extracting(transition -> transition.getEvent().getName())
        .containsExactly("CONCLUDE");
    assertThat(rows.guardWirings())
        .extracting(GuardWiring::getTypeKey)
        .containsExactly(APPROVED_GUARD, TERMINAL_GUARD);
    assertThat(rows.actionWirings())
        .extracting(ActionWiring::getTypeKey)
        .containsExactly(VISIBILITY_ACTION);
  }

  /**
   * One query must return both Guard Wiring scopes. A definition-wide Guard has a null {@code
   * transition_id}, so an inner join on that column would drop the rows that apply to every
   * Transition. The graph would still compile and would check fewer Guards than configured.
   */
  @Test
  @DisplayName("both Guard scopes come back from the one query, the definition-wide one included")
  void load_readsBothGuardScopes() {
    DefinitionVersionRows rows = loader.load(subjectId);

    assertThat(rows.guardWirings())
        .filteredOn(wiring -> wiring.getTransition() == null)
        .extracting(GuardWiring::getTypeKey)
        .containsExactly(APPROVED_GUARD);
    assertThat(rows.guardWirings())
        .filteredOn(wiring -> wiring.getTransition() != null)
        .extracting(GuardWiring::getTypeKey)
        .containsExactly(TERMINAL_GUARD);
  }

  /**
   * A Definition Version Pin naming a missing row is graph corruption, not an absence, because no
   * Transition of that Lifecycle can be evaluated. The loader throws the same exception as for rows
   * that do not compile, because a caller can do as little about either.
   */
  @Test
  @DisplayName("an id no Definition Version has is refused, and the refusal names it")
  void load_whenNoVersionHasThatId_isRefused() {
    long absent = subjectId + 10_000;

    assertThatThrownBy(() -> loader.load(absent))
        .isInstanceOf(InvalidGraphException.class)
        .hasMessageContaining(String.valueOf(absent));
  }

  /**
   * Six is one query per table. The three queries with associations fetch them, so reading a
   * Transition's from-State, to-State and Event costs no further query and there is no N+1.
   *
   * <p>The count covers the compile as well as the load, because a lazy association only runs its
   * query when something dereferences it, and the compile does.
   *
   * <p>The test clears the persistence context first. Otherwise the rows it just wrote are still
   * managed, {@code findById} answers from the first-level cache, and the count is one short for a
   * reason unrelated to the loader.
   */
  @Test
  @DisplayName("loading and compiling one version costs six queries, one per table")
  void load_issuesOneQueryPerTable() {
    entityManager.clear();
    Statistics statistics = statistics();
    statistics.clear();

    compile(loader.load(subjectId));

    assertThat(statistics.getPrepareStatementCount())
        .withFailMessage(
            "Loading and compiling one Definition Version issued %d statements, expected %d - one"
                + " per table. More than that is an association being followed one row at a time.",
            statistics.getPrepareStatementCount(), QUERIES_PER_LOAD)
        .isEqualTo(QUERIES_PER_LOAD);
  }

  /**
   * The loader must return rows the compiler accepts. Two faults would break that without showing
   * in the lists. A row whose owning version is a different instance of the same database row makes
   * the compiler refuse the rows as spanning two versions. A Wiring row grouped by a Transition
   * instance other than the one in {@link DefinitionVersionRows#transitions()} loses its chain
   * without an error.
   */
  @Test
  @DisplayName("the rows it loads compile, chains and all")
  void load_producesRowsThatCompile() {
    CompiledGraph graph = compile(loader.load(subjectId));

    assertThat(graph.definitionVersionId()).isEqualTo(subjectId);
    assertThat(graph.initialState()).isEqualTo("SUBMITTED");
    assertThat(graph.isTerminal("CONCLUDED")).isTrue();
    assertThat(graph.declaresEvent("OVERRIDE")).isTrue();
    assertThat(graph.transitionsFrom("CONCLUDED")).isEmpty();

    var concluding = graph.transition("SUBMITTED", "CONCLUDE").orElseThrow();
    assertThat(concluding.toState()).isEqualTo("CONCLUDED");
    assertThat(concluding.requiredAuthority()).isEqualTo(Set.of(RequiredAuthority.IS_ADMIN));
    assertThat(concluding.guards())
        .withFailMessage("the definition-wide Guard comes first, and both are bound")
        .extracting(GuardStep::typeKey)
        .containsExactly(APPROVED_GUARD, TERMINAL_GUARD);
    assertThat(concluding.actions())
        .extracting(ActionStep::typeKey)
        .containsExactly(VISIBILITY_ACTION);
  }

  /** The {@code jsonb} column reaches the catalogue as written, not as {@code null}. */
  @Test
  @DisplayName("a Wiring row's params reach the catalogue")
  void load_readsWiringParams() {
    DefinitionVersionRows rows = loader.load(subjectId);

    assertThat(rows.actionWirings())
        .singleElement()
        .extracting(ActionWiring::getParams)
        .asString()
        .contains("BOTH");
  }

  /**
   * The subject has two States, two Events (one without a Transition), one Transition, a Guard in
   * each scope, and an Action. That puts at least one row in every table the loader reads.
   */
  private long writeSubject() {
    LifecycleDefinition version = definitions.save(definitionIn(STANDARD_FAMILY));
    State submitted = states.save(stateBuilder(version, "SUBMITTED").initial(true).build());
    State concluded = states.save(stateBuilder(version, "CONCLUDED").terminal(true).build());
    Event conclude = events.save(eventIn(version, "CONCLUDE"));
    events.save(eventIn(version, "OVERRIDE"));
    Transition concluding =
        transitions.save(
            transitionOf(
                version, submitted, conclude, concluded, Set.of(RequiredAuthority.IS_ADMIN)));
    guardWirings.save(guardWiring(version, null, APPROVED_GUARD, 1));
    guardWirings.save(guardWiring(version, concluding, TERMINAL_GUARD, 1));
    actionWirings.save(
        ActionWiring.builder()
            .transition(concluding)
            .typeKey(VISIBILITY_ACTION)
            .params(VISIBILITY_PARAMS)
            .sortOrder(1)
            .build());
    return version.getId();
  }

  /**
   * A second Definition Version with rows in all five tables. Its Transition is a self-loop, which
   * the schema allows, so it needs one State. No test reads it; it only has to be what a loader
   * reading by the wrong key would return.
   */
  private void writeTheOtherVersion() {
    LifecycleDefinition other = definitions.save(definitionIn(OTHER_FAMILY));
    State only = states.save(stateIn(other, "ELSEWHERE"));
    Event loop = events.save(eventIn(other, "LOOP"));
    Transition selfLoop =
        transitions.save(transitionOf(other, only, loop, only, Set.of(RequiredAuthority.IS_ADMIN)));
    guardWirings.save(guardWiring(other, null, TERMINAL_GUARD, 1));
    actionWirings.save(
        ActionWiring.builder()
            .transition(selfLoop)
            .typeKey(VISIBILITY_ACTION)
            .sortOrder(1)
            .build());
  }

  private static Transition transitionOf(
      LifecycleDefinition version,
      State from,
      Event event,
      State to,
      Set<RequiredAuthority> requiredAuthority) {
    return Transition.builder()
        .lifecycleDefinition(version)
        .fromState(from)
        .event(event)
        .toState(to)
        .requiredAuthority(requiredAuthority)
        .build();
  }

  private static GuardWiring guardWiring(
      LifecycleDefinition version, Transition transition, String typeKey, int sortOrder) {
    return GuardWiring.builder()
        .lifecycleDefinition(version)
        .transition(transition)
        .typeKey(typeKey)
        .sortOrder(sortOrder)
        .build();
  }

  /**
   * Compiles with catalogues that bind any key, because this class tests which rows arrive and in
   * what shape, not what a strategy does. The integration test runs the real registries over the
   * same rows.
   */
  private static CompiledGraph compile(DefinitionVersionRows rows) {
    return new DefinitionCompiler(
            DefinitionFixtures.bindingAnyGuard(), DefinitionFixtures.bindingAnyAction())
        .compile(rows);
  }

  /**
   * Hibernate's statement counter, switched on here instead of in configuration because it is off
   * by default, has a cost, and only one test in this repository needs it.
   */
  private Statistics statistics() {
    Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    statistics.setStatisticsEnabled(true);
    return statistics;
  }
}
