package eu.bbmri_eric.negotiator.lifecycle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import eu.bbmri_eric.negotiator.lifecycle.definition.DefinitionResolutionException;
import eu.bbmri_eric.negotiator.lifecycle.definition.LifecycleDefinitions;
import eu.bbmri_eric.negotiator.lifecycle.graph.ActionStep;
import eu.bbmri_eric.negotiator.lifecycle.graph.CompiledGraph;
import eu.bbmri_eric.negotiator.lifecycle.graph.CompiledTransition;
import eu.bbmri_eric.negotiator.lifecycle.graph.GuardStep;
import eu.bbmri_eric.negotiator.lifecycle.graph.InvalidGraphException;
import eu.bbmri_eric.negotiator.lifecycle.graph.RequiredAuthority;
import eu.bbmri_eric.negotiator.util.IntegrationTest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Compiled Graph resolution as code outside the definition package sees it: a Definition Version
 * id in, a {@link CompiledGraph} out, against rows this test wrote.
 *
 * <p>The class sits outside {@code lifecycle.definition} on purpose. From here only {@link
 * LifecycleDefinitions} and the {@code graph} types are reachable, so the class compiling proves
 * that the public entry point is enough. It writes its rows as SQL because the entities are package
 * private, which also makes the assertions rest on the schema rather than on the entity mapping.
 *
 * <p>The tests commit. The compiler compares owning versions by reference, so the loader must read
 * all six tables in one transaction. {@code @DataJpaTest} wraps every test in a transaction and
 * would hide a missing {@code @Transactional} on {@code DefinitionVersionLoader.load}. The tests
 * here that compile a graph fail without it.
 */
@IntegrationTest
class CompiledGraphResolutionIntegrationTest {

  private static final String NEGOTIATION_FAMILY = "standard-negotiation-flow";
  private static final String RESOURCE_FAMILY = "standard-resource-flow";
  private static final String A_SECOND_FAMILY = "expedited-negotiation-flow";

  /**
   * Two real Guard keys and one real Action key. Which strategies they name does not matter, only
   * that a definition-wide row and a transition-scoped row arrive as one chain in that order.
   */
  private static final String DEFINITION_WIDE_GUARD = "TERMINAL_AGGREGATION";

  private static final String TRANSITION_GUARD = "NEGOTIATION_APPROVED";
  private static final String VISIBILITY_ACTION = "SET_POST_VISIBILITY";
  private static final String VISIBILITY_PARAMS = "{\"scope\":\"BOTH\",\"enabled\":true}";

  @Autowired LifecycleDefinitions definitions;
  @Autowired JdbcTemplate jdbc;

  /**
   * Every version this test wrote, so that {@link #dropTheRowsThisTestWrote()} takes back exactly
   * what it added and nothing else.
   */
  private final List<Long> versionsWritten = new ArrayList<>();

  /**
   * Scoped to this test's own versions rather than {@code DELETE FROM lifecycle_definition}, which
   * is what it said first and would have been a quiet trap: the tables are empty in every
   * environment <em>today</em>, so an unqualified delete is correct and stays correct right up
   * until the migration slab lands ADR 0009's v1 seed — at which point this class would wipe it for
   * every test that ran afterwards, and the failure would surface anywhere but here.
   *
   * <p>Children first, and {@code action_wiring} through its Transition, because that table carries
   * no definition column of its own.
   *
   * <p>What is deliberately <em>not</em> cleaned is the Compiled Graph cache, which is a singleton
   * of the shared application context and goes on holding graphs for versions these deletes have
   * removed. That is harmless only because the sequence never reissues an id, so no later test can
   * ask for one of these versions and be answered from the cache.
   */
  @AfterEach
  void dropTheRowsThisTestWrote() {
    for (long versionId : versionsWritten) {
      jdbc.update(
          """
          DELETE FROM action_wiring
           WHERE transition_id IN (SELECT id FROM transition WHERE lifecycle_definition_id = ?)
          """,
          versionId);
      jdbc.update("DELETE FROM guard_wiring WHERE lifecycle_definition_id = ?", versionId);
      jdbc.update("DELETE FROM transition WHERE lifecycle_definition_id = ?", versionId);
      jdbc.update("DELETE FROM state WHERE lifecycle_definition_id = ?", versionId);
      jdbc.update("DELETE FROM event WHERE lifecycle_definition_id = ?", versionId);
      jdbc.update("DELETE FROM lifecycle_definition WHERE id = ?", versionId);
    }
    versionsWritten.clear();
  }

  @Test
  @DisplayName("a Definition Version's rows become the graph the evaluator would be handed")
  void graphFor_compilesTheVersionsRows() {
    long versionId = writeTheStandardNegotiationFlow(NEGOTIATION_FAMILY, 1);

    CompiledGraph graph = definitions.graphFor(versionId);

    assertThat(graph.definitionVersionId()).isEqualTo(versionId);
    assertThat(graph.initialState()).isEqualTo("SUBMITTED");
    assertThat(graph.states())
        .containsExactlyInAnyOrder("SUBMITTED", "IN_PROGRESS", "CONCLUDED", "APPROVED");
    assertThat(graph.events()).containsExactlyInAnyOrder("APPROVE", "CONCLUDE", "OVERRIDE");
    assertThat(graph.isTerminal("CONCLUDED")).isTrue();
    assertThat(graph.isTerminal("IN_PROGRESS")).isFalse();

    assertThat(graph.transitionsFrom("APPROVED"))
        .withFailMessage("a Legacy State is declared and leads nowhere, rather than being absent")
        .isEmpty();

    CompiledTransition approve = graph.transition("SUBMITTED", "APPROVE").orElseThrow();
    assertThat(approve.toState()).isEqualTo("IN_PROGRESS");
    assertThat(approve.requiredAuthority()).isEqualTo(RequiredAuthority.IS_ADMIN);
    assertThat(approve.actions())
        .extracting(ActionStep::typeKey)
        .containsExactly(VISIBILITY_ACTION);
  }

  /** A Transition with no Guard of its own still gets the definition-wide one. */
  @Test
  @DisplayName("both Guard scopes reach the chain, definition-wide first")
  void graphFor_foldsBothGuardScopesIntoOneChain() {
    long versionId = writeTheStandardNegotiationFlow(NEGOTIATION_FAMILY, 1);

    CompiledGraph graph = definitions.graphFor(versionId);

    assertThat(chainOf(graph, "IN_PROGRESS", "CONCLUDE"))
        .containsExactly(DEFINITION_WIDE_GUARD, TRANSITION_GUARD);
    assertThat(chainOf(graph, "SUBMITTED", "APPROVE")).containsExactly(DEFINITION_WIDE_GUARD);
  }

  @Test
  @DisplayName("a version asked for twice is answered with the graph already compiled")
  void graphFor_keepsTheGraphItCompiled() {
    long versionId = writeTheStandardNegotiationFlow(NEGOTIATION_FAMILY, 1);

    assertThat(definitions.graphFor(versionId)).isSameAs(definitions.graphFor(versionId));
  }

  /**
   * Three pins over two versions give two entries. Each entry is the same instance a single
   * resolution returns, which shows both come from one cache rather than from separate compiles.
   */
  @Test
  @DisplayName("a Negotiation's Resource pins resolve to one graph per distinct version")
  void graphsFor_resolvesOneGraphPerDistinctPin() {
    long standard = writeTheStandardNegotiationFlow(NEGOTIATION_FAMILY, 1);
    long expedited = writeTheStandardNegotiationFlow(A_SECOND_FAMILY, 1);

    Map<Long, CompiledGraph> graphs = definitions.graphsFor(List.of(standard, expedited, standard));

    assertThat(graphs).hasSize(2).containsOnlyKeys(standard, expedited);
    assertThat(graphs.get(standard)).isSameAs(definitions.graphFor(standard));
    assertThat(graphs.get(expedited)).isSameAs(definitions.graphFor(expedited));
  }

  /**
   * The schema allows a version with zero initial States, because a version being authored passes
   * through that shape, so that is the broken version this test writes.
   *
   * <p>Nothing caches a failure, so the {@code UPDATE} below takes effect on the next request and a
   * mis-seeded definition can be fixed without a restart.
   */
  @Test
  @DisplayName("a version that cannot compile is refused every time, and is served once repaired")
  void graphFor_doesNotRememberAVersionThatCouldNotCompile() {
    long versionId = writeVersion(NEGOTIATION_FAMILY, "NEGOTIATION", 1, true, false);
    long orphan = writeState(versionId, "SUBMITTED", false, false);

    assertThatThrownBy(() -> definitions.graphFor(versionId))
        .isInstanceOf(InvalidGraphException.class)
        .hasMessageContaining("initial State");
    assertThatThrownBy(() -> definitions.graphFor(versionId))
        .isInstanceOf(InvalidGraphException.class);

    jdbc.update("UPDATE state SET initial = TRUE WHERE id = ?", orphan);

    assertThat(definitions.graphFor(versionId).initialState()).isEqualTo("SUBMITTED");
  }

  /**
   * A Lifecycle whose pinned version is gone cannot be evaluated, so a missing row counts as graph
   * corruption. It throws the same exception as a version that does not compile, because a caller
   * can do nothing different about either.
   */
  @Test
  @DisplayName("a pin naming no Definition Version is refused, and the refusal names the id")
  void graphFor_whenNoVersionHasThatId_isRefused() {
    assertThatThrownBy(() -> definitions.graphFor(404_404L))
        .isInstanceOf(InvalidGraphException.class)
        .hasMessageContaining("404404");
  }

  /**
   * The answer is the row id that becomes the Definition Version Pin. Each family also gets an
   * inactive version, because picking the active one is what the two finders have to do.
   */
  @Test
  @DisplayName("both Definition Scopes resolve, and answer the active version's row id")
  void resolution_answersBothScopesFromOutsideThePackage() {
    writeVersion(NEGOTIATION_FAMILY, "NEGOTIATION", 1, false, false);
    long activeNegotiation = writeVersion(NEGOTIATION_FAMILY, "NEGOTIATION", 2, true, false);
    writeVersion(RESOURCE_FAMILY, "RESOURCE", 1, false, true);
    long activeResource = writeVersion(RESOURCE_FAMILY, "RESOURCE", 2, true, true);

    assertThat(definitions.resolveForNegotiation()).isEqualTo(activeNegotiation);
    assertThat(definitions.resolveForResource()).isEqualTo(activeResource);
  }

  /**
   * The exception is public so a caller can tell a missing definition apart from any other runtime
   * failure.
   *
   * <p>This test asserts that nothing is seeded. A migration that seeds an active version of each
   * Definition Scope makes both assertions fail, and the test then needs rewriting, not repair.
   */
  @Test
  @DisplayName("with nothing seeded, resolution is refused by name")
  void resolution_whenNothingIsSeeded_isRefused() {
    assertThatThrownBy(() -> definitions.resolveForNegotiation())
        .isInstanceOf(DefinitionResolutionException.class);
    assertThatThrownBy(() -> definitions.resolveForResource())
        .isInstanceOf(DefinitionResolutionException.class);
  }

  private static List<String> chainOf(CompiledGraph graph, String fromState, String event) {
    return graph.transition(fromState, event).orElseThrow().guards().stream()
        .map(GuardStep::typeKey)
        .toList();
  }

  /**
   * A whole Definition Version with rows in each of the six tables, including a Legacy State no
   * Transition reaches, an Event with no Transition, a terminal State, a Guard in each scope, and an
   * Action with {@code jsonb} configuration.
   *
   * @return the new version's row id
   */
  private long writeTheStandardNegotiationFlow(String familyKey, int version) {
    long versionId = writeVersion(familyKey, "NEGOTIATION", version, true, false);
    long submitted = writeState(versionId, "SUBMITTED", true, false);
    long inProgress = writeState(versionId, "IN_PROGRESS", false, false);
    long concluded = writeState(versionId, "CONCLUDED", false, true);
    writeState(versionId, "APPROVED", false, false);
    long approve = writeEvent(versionId, "APPROVE");
    long conclude = writeEvent(versionId, "CONCLUDE");
    writeEvent(versionId, "OVERRIDE");

    long approving = writeTransition(versionId, submitted, approve, inProgress, "IS_ADMIN");
    long concluding = writeTransition(versionId, inProgress, conclude, concluded, "IS_ADMIN");

    writeGuardWiring(versionId, null, DEFINITION_WIDE_GUARD, 1);
    writeGuardWiring(versionId, concluding, TRANSITION_GUARD, 1);
    writeActionWiring(approving, VISIBILITY_ACTION, VISIBILITY_PARAMS, 1);
    return versionId;
  }

  private long writeVersion(
      String familyKey, String scope, int version, boolean active, boolean globalDefault) {
    long versionId =
        jdbc.queryForObject(
            """
            INSERT INTO lifecycle_definition (scope, family_key, name, version, active,
                                              is_global_default)
            VALUES (?, ?, ?, ?, ?, ?) RETURNING id
            """,
            Long.class,
            scope,
            familyKey,
            familyKey,
            version,
            active,
            globalDefault);
    versionsWritten.add(versionId);
    return versionId;
  }

  private long writeState(long versionId, String name, boolean initial, boolean terminal) {
    return jdbc.queryForObject(
        """
        INSERT INTO state (lifecycle_definition_id, name, label, initial, terminal)
        VALUES (?, ?, ?, ?, ?) RETURNING id
        """,
        Long.class,
        versionId,
        name,
        name,
        initial,
        terminal);
  }

  private long writeEvent(long versionId, String name) {
    return jdbc.queryForObject(
        "INSERT INTO event (lifecycle_definition_id, name) VALUES (?, ?) RETURNING id",
        Long.class,
        versionId,
        name);
  }

  /**
   * The column list is written {@code (from, event, to)} to match the parameters, rather than in
   * the table's own {@code (from, to, event)} order. Both are three interchangeable {@code bigint}s
   * — transposing two would insert a different edge, compile without complaint, and be caught only
   * by whichever assertion happened to name that Transition.
   */
  private long writeTransition(
      long versionId, long fromStateId, long eventId, long toStateId, String requiredAuthority) {
    return jdbc.queryForObject(
        """
        INSERT INTO transition (lifecycle_definition_id, from_state_id, event_id, to_state_id,
                                required_authority)
        VALUES (?, ?, ?, ?, ?) RETURNING id
        """,
        Long.class,
        versionId,
        fromStateId,
        eventId,
        toStateId,
        requiredAuthority);
  }

  /**
   * A null {@code transitionId} is how "every Transition of this version" is spelled. Cast
   * explicitly, because that is the one parameter here that is ever null and PostgreSQL will not
   * infer a type for an untyped null.
   */
  private void writeGuardWiring(long versionId, Long transitionId, String typeKey, int sortOrder) {
    jdbc.update(
        """
        INSERT INTO guard_wiring (lifecycle_definition_id, transition_id, type_key, sort_order)
        VALUES (?, CAST(? AS bigint), ?, ?)
        """,
        versionId,
        transitionId,
        typeKey,
        sortOrder);
  }

  private void writeActionWiring(long transitionId, String typeKey, String params, int sortOrder) {
    jdbc.update(
        """
        INSERT INTO action_wiring (transition_id, type_key, params, sort_order)
        VALUES (?, ?, CAST(? AS jsonb), ?)
        """,
        transitionId,
        typeKey,
        params,
        sortOrder);
  }
}
