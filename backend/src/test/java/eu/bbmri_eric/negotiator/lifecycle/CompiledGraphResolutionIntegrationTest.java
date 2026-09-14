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
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Compiled Graph resolution as everything outside the definition package sees it: a Definition
 * Version id in, a {@link CompiledGraph} out, against rows this test wrote.
 *
 * <p><b>It is in this package rather than in {@code lifecycle.definition} on purpose</b>, and that
 * is most of what it proves. Every other test of this subsystem sits inside the package whose types
 * it exercises, which is exactly why none of them could have noticed that nothing outside could
 * reach any of it. From here, {@link LifecycleDefinitions} and the {@code graph} vocabulary are the
 * only things nameable — the six entities, their repositories, the compiler and the cache are all
 * package private and remain so. That the class compiles is a real assertion.
 *
 * <p><b>It writes its rows as SQL for the same reason.</b> The entity builders are not reachable
 * from out here, which leaves the tables themselves — which is how the v1 seed will arrive anyway
 * (ADR 0009 fixes it as frozen SQL), and which means these assertions rest on the schema rather
 * than on a mapping that agrees with itself.
 *
 * <p><b>And it commits, which is the other thing an in-package test cannot do.</b>
 * {@code @DataJpaTest} wraps each test in a transaction, so the loader's own is redundant there and
 * its absence would go unnoticed; the compiler compares owning versions by reference and groups
 * Wiring by Transition identity, so six queries in six persistence contexts produce rows it refuses
 * as straddling two versions.
 *
 * <p>That was run rather than reasoned about. With {@code @Transactional} taken off {@code
 * DefinitionVersionLoader.load}, {@code DefinitionVersionLoaderTest} stays green in full and the
 * five tests below that compile a graph all fail with "These rows belong to a different Definition
 * Version". Nothing here asserts it directly, because a test that removed the annotation would be
 * asserting on Spring; what makes it safe is that this class exists and commits.
 *
 * <p>The six definition tables are empty in every environment until the migration slab lands the v1
 * seed, so each test writes what it needs and {@link #dropTheRowsThisTestWrote()} takes it away
 * again: the resolution questions are asked of the whole table, and a version left active would be
 * answered to the next test rather than to nobody.
 */
@IntegrationTest
class CompiledGraphResolutionIntegrationTest {

  private static final String NEGOTIATION_FAMILY = "standard-negotiation-flow";
  private static final String RESOURCE_FAMILY = "standard-resource-flow";
  private static final String A_SECOND_FAMILY = "expedited-negotiation-flow";

  /**
   * Two real Guard keys and one real Action key. Which strategies they are does not matter here,
   * and the pair is not a Guard chain anyone would publish — what is under test is that a
   * definition-wide row and a transition-scoped row arrive as one chain in that order, which is the
   * one thing about Wiring that no row can state about itself.
   */
  private static final String DEFINITION_WIDE_GUARD = "TERMINAL_AGGREGATION";

  private static final String TRANSITION_GUARD = "NEGOTIATION_APPROVED";
  private static final String VISIBILITY_ACTION = "SET_POST_VISIBILITY";
  private static final String VISIBILITY_PARAMS = "{\"scope\":\"BOTH\",\"enabled\":true}";

  @Autowired LifecycleDefinitions definitions;
  @Autowired JdbcTemplate jdbc;

  /**
   * The six tables, written to directly. Shared with {@code EventFiringIntegrationTest}, which
   * needs the same six writers — see {@link DefinitionRows} for why they are SQL and not entities.
   */
  private DefinitionRows rows;

  @BeforeEach
  void startWithNothingWritten() {
    rows = new DefinitionRows(jdbc);
  }

  /**
   * Takes back exactly what this test added and nothing else, because this class shares its
   * application context with the rest of the suite and a version left active would be answered to
   * the next test rather than to nobody. {@link DefinitionRows#deleteEverythingWritten()} carries
   * the reasoning, including why an unqualified delete would be a quiet trap once ADR 0009's v1
   * seed lands.
   */
  @AfterEach
  void dropTheRowsThisTestWrote() {
    rows.deleteEverythingWritten();
  }

  /**
   * The slab's first criterion, end to end: rows written here, read and compiled by production
   * code, and answered as the graph an evaluator is handed.
   */
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

  /**
   * The two Guard scopes, folded. A row saying "every Transition of this version" and a row saying
   * "this Transition" have to arrive as one ordered chain, definition-wide first, and the edge that
   * carries no Guard of its own still carries the definition-wide one.
   */
  @Test
  @DisplayName("both Guard scopes reach the chain, definition-wide first")
  void graphFor_foldsBothGuardScopesIntoOneChain() {
    long versionId = writeTheStandardNegotiationFlow(NEGOTIATION_FAMILY, 1);

    CompiledGraph graph = definitions.graphFor(versionId);

    assertThat(chainOf(graph, "IN_PROGRESS", "CONCLUDE"))
        .containsExactly(DEFINITION_WIDE_GUARD, TRANSITION_GUARD);
    assertThat(chainOf(graph, "SUBMITTED", "APPROVE")).containsExactly(DEFINITION_WIDE_GUARD);
  }

  /** One graph per version, kept: the second caller pays for nothing the first already did. */
  @Test
  @DisplayName("a version asked for twice is answered with the graph already compiled")
  void graphFor_keepsTheGraphItCompiled() {
    long versionId = writeTheStandardNegotiationFlow(NEGOTIATION_FAMILY, 1);

    assertThat(definitions.graphFor(versionId)).isSameAs(definitions.graphFor(versionId));
  }

  /**
   * The batched resolution, in the shape it exists for: one Negotiation's Resources, each pinned to
   * its own Definition Version, most of them sharing one. Three pins, two versions, two entries —
   * and each entry is the very graph a single resolution answers with, which is what says they came
   * from one cache rather than from three compiles.
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
   * A version too broken to compile fails, and fails <em>again</em> rather than being remembered as
   * broken. Zero initial States is the one way to spell that which the schema allows: the partial
   * unique index stops a second one, and permits none at all because a version being authored
   * passes through that state.
   *
   * <p>The repair is the point. Nothing caches a failure, so the {@code UPDATE} below is served on
   * the next request without a restart — which is what makes a mis-seeded definition an incident
   * someone fixes rather than one they redeploy through.
   */
  @Test
  @DisplayName("a version that cannot compile is refused every time, and is served once repaired")
  void graphFor_doesNotRememberAVersionThatCouldNotCompile() {
    long versionId = rows.writeVersion(NEGOTIATION_FAMILY, "NEGOTIATION", 1, true, false);
    long orphan = rows.writeState(versionId, "SUBMITTED", false, false);

    assertThatThrownBy(() -> definitions.graphFor(versionId))
        .isInstanceOf(InvalidGraphException.class)
        .hasMessageContaining("initial State");
    assertThatThrownBy(() -> definitions.graphFor(versionId))
        .isInstanceOf(InvalidGraphException.class);

    jdbc.update("UPDATE state SET initial = TRUE WHERE id = ?", orphan);

    assertThat(definitions.graphFor(versionId).initialState()).isEqualTo("SUBMITTED");
  }

  /**
   * A Definition Version Pin naming a row that is not there. Reported as graph corruption rather
   * than as an absence, because a Lifecycle whose pinned version has gone cannot be judged at all —
   * and reported as the same exception a version that will not compile reports, since there is
   * nothing different a caller could do about either.
   */
  @Test
  @DisplayName("a pin naming no Definition Version is refused, and the refusal names the id")
  void graphFor_whenNoVersionHasThatId_isRefused() {
    assertThatThrownBy(() -> definitions.graphFor(404_404L))
        .isInstanceOf(InvalidGraphException.class)
        .hasMessageContaining("404404");
  }

  /**
   * Definition Resolution, from outside, for both Definition Scopes — the answer being the row id
   * that goes on to be written as a Definition Version Pin.
   *
   * <p>An inactive version of each family is written alongside, because that is the question the
   * two finders actually answer and a fixture holding only the active row would not ask it.
   */
  @Test
  @DisplayName("both Definition Scopes resolve, and answer the active version's row id")
  void resolution_answersBothScopesFromOutsideThePackage() {
    rows.writeVersion(NEGOTIATION_FAMILY, "NEGOTIATION", 1, false, false);
    long activeNegotiation = rows.writeVersion(NEGOTIATION_FAMILY, "NEGOTIATION", 2, true, false);
    rows.writeVersion(RESOURCE_FAMILY, "RESOURCE", 1, false, true);
    long activeResource = rows.writeVersion(RESOURCE_FAMILY, "RESOURCE", 2, true, true);

    assertThat(definitions.resolveForNegotiation()).isEqualTo(activeNegotiation);
    assertThat(definitions.resolveForResource()).isEqualTo(activeResource);
  }

  /**
   * The answer the deployment actually gives today, and the reason the exception had to become
   * public: the six tables are empty in every environment until the seed lands, so this is the
   * ordinary path rather than a remote one, and a caller that cannot name what it caught cannot
   * tell it apart from any other runtime failure.
   *
   * <p><b>This test asserts an absence, and the migration slab ends it.</b> Once ADR 0009's v1 seed
   * lands there will be an active version of each Scope in every environment, and both assertions
   * below become false — correctly. It is the one test in this class that the seed invalidates
   * rather than merely joins, so it is expected to be rewritten in that slab's diff, not repaired
   * in passing.
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
   * A whole Definition Version with one row in each of the six tables and one of every shape that
   * has to survive the trip: a Legacy State no Transition reaches, an Event carrying no Transition,
   * a terminal State, a Guard in each scope, and an Action with {@code jsonb} configuration.
   *
   * @return the new version's row id
   */
  private long writeTheStandardNegotiationFlow(String familyKey, int version) {
    long versionId = rows.writeVersion(familyKey, "NEGOTIATION", version, true, false);
    long submitted = rows.writeState(versionId, "SUBMITTED", true, false);
    long inProgress = rows.writeState(versionId, "IN_PROGRESS", false, false);
    long concluded = rows.writeState(versionId, "CONCLUDED", false, true);
    rows.writeState(versionId, "APPROVED", false, false);
    long approve = rows.writeEvent(versionId, "APPROVE");
    long conclude = rows.writeEvent(versionId, "CONCLUDE");
    rows.writeEvent(versionId, "OVERRIDE");

    long approving = rows.writeTransition(versionId, submitted, approve, inProgress, "IS_ADMIN");
    long concluding = rows.writeTransition(versionId, inProgress, conclude, concluded, "IS_ADMIN");

    rows.writeGuardWiring(versionId, null, DEFINITION_WIDE_GUARD, 1);
    rows.writeGuardWiring(versionId, concluding, TRANSITION_GUARD, 1);
    rows.writeActionWiring(approving, VISIBILITY_ACTION, VISIBILITY_PARAMS, 1);
    return versionId;
  }
}
