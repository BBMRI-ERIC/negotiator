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
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * The load half of "loading the definition graph is an explicit, testable step": which rows come
 * back for one Definition Version id, against a real PostgreSQL.
 *
 * <p>Two Definition Versions are written before every test and only one is ever asked for. That is
 * not belt and braces — a loader whose {@code WHERE} clause is wrong reads a graph quietly mixing
 * two versions, and against a single-version fixture every assertion below would still pass. The
 * second version carries a row in all five tables for the same reason, including the one reached
 * through a join rather than through a definition column.
 *
 * <p>What this class deliberately cannot prove is the loader's transaction. {@code @DataJpaTest}
 * wraps each test method in one, so the persistence-context identity {@link DefinitionCompiler}
 * depends on holds here whether or not {@link DefinitionVersionLoader#load} is annotated at all —
 * checked by removing the annotation, at which point every test here is still green and five of
 * {@code CompiledGraphResolutionIntegrationTest}'s eight are not. That class is where the
 * transaction is answered for, by committing; this one is where the queries are.
 */
@RepositoryTest
@Import(DefinitionVersionLoader.class)
class DefinitionVersionLoaderTest {

  /** One query per table, and the number the cache's javadoc anticipated. */
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
   * The Guard Wiring scopes, which one query has to return together. A definition-wide Guard is
   * spelled as a null {@code transition_id}, so an inner join on that column would drop exactly the
   * rows that apply to every Transition — and drop them silently, leaving a graph that compiles and
   * gates less than it was configured to.
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
   * A Definition Version Pin naming a row that is not there is graph corruption, not an absence: it
   * is a Lifecycle that cannot be judged at all. Reported as the same exception a version whose
   * rows will not compile reports, because a caller can do exactly as much about either.
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
   * The count the slab pins. Six is one query per table and no more: the three queries with
   * associations worth following fetch them, so reading a Transition's from-State, to-State and
   * Event costs nothing further — which is the whole difference between this and the N+1 ADR 0001
   * says an engine owning its own persistence would have.
   *
   * <p>Counted across the compile as well as the load, because a lazy association would not show up
   * until something dereferenced it, and compiling is what does.
   *
   * <p>The persistence context is cleared first. Without that, the rows this test has just written
   * are still managed, {@code findById} answers from the first-level cache, and the count is short
   * by one for a reason that has nothing to do with the loader.
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
   * The loader's real contract, which is not "six lists" but "rows a compile accepts". Two things
   * would break it and neither is visible in the lists themselves: a row whose owning version is a
   * different instance of the same database row, which the compiler reads as a version-straddling
   * definition and refuses; and a Wiring row grouped by a Transition instance that is not the one
   * in {@link DefinitionVersionRows#transitions()}, which silently loses the chain instead.
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
    assertThat(concluding.requiredAuthority()).isEqualTo(RequiredAuthority.IS_ADMIN);
    assertThat(concluding.guards())
        .withFailMessage("the definition-wide Guard comes first, and both are bound")
        .extracting(GuardStep::typeKey)
        .containsExactly(APPROVED_GUARD, TERMINAL_GUARD);
    assertThat(concluding.actions())
        .extracting(ActionStep::typeKey)
        .containsExactly(VISIBILITY_ACTION);
  }

  /** The {@code jsonb} column reaches the catalogue as written, rather than as {@code null}. */
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
   * The subject: two States, two Events — one of them carrying no Transition — one Transition, a
   * Guard in each of the two scopes, and an Action. Small, but with one row in every table the
   * loader reads and one of every shape that has ever been got wrong.
   */
  private long writeSubject() {
    LifecycleDefinition version = definitions.save(definitionIn(STANDARD_FAMILY));
    State submitted = states.save(stateBuilder(version, "SUBMITTED").initial(true).build());
    State concluded = states.save(stateBuilder(version, "CONCLUDED").terminal(true).build());
    Event conclude = events.save(eventIn(version, "CONCLUDE"));
    events.save(eventIn(version, "OVERRIDE"));
    Transition concluding =
        transitions.save(
            transitionOf(version, submitted, conclude, concluded, RequiredAuthority.IS_ADMIN));
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
   * A whole second Definition Version, in all five tables. Its Transition is a self-loop, which the
   * schema permits and which keeps it to one State: nothing here is asked anything, and the only
   * property it has to have is that a loader reading by the wrong key would return it.
   */
  private void writeTheOtherVersion() {
    LifecycleDefinition other = definitions.save(definitionIn(OTHER_FAMILY));
    State only = states.save(stateIn(other, "ELSEWHERE"));
    Event loop = events.save(eventIn(other, "LOOP"));
    Transition selfLoop =
        transitions.save(transitionOf(other, only, loop, only, RequiredAuthority.NONE));
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
      RequiredAuthority requiredAuthority) {
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
   * Compiles with catalogues that bind any key, because what is under test is which rows arrived
   * and in what shape, never what a strategy does. The real registries are exercised over the same
   * rows by the integration test, which is also the only place their key spaces matter.
   */
  private static CompiledGraph compile(DefinitionVersionRows rows) {
    return new DefinitionCompiler(
            DefinitionFixtures.bindingAnyGuard(), DefinitionFixtures.bindingAnyAction())
        .compile(rows);
  }

  /**
   * Hibernate's own statement counter, switched on here rather than in configuration: it is off by
   * default and costs something, and exactly one test in this repository needs it.
   */
  private Statistics statistics() {
    Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    statistics.setStatisticsEnabled(true);
    return statistics;
  }
}
