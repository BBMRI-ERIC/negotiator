package eu.bbmri_eric.negotiator.characterization.service;

import static eu.bbmri_eric.negotiator.characterization.service.LifecycleHistory.negotiationRecordsOf;
import static eu.bbmri_eric.negotiator.characterization.service.SeededResourceSubject.ADMIN;
import static eu.bbmri_eric.negotiator.characterization.service.SeededResourceSubject.ANOTHER_RESOURCE_ROW_ID;
import static eu.bbmri_eric.negotiator.characterization.service.SeededResourceSubject.CREATOR;
import static eu.bbmri_eric.negotiator.characterization.service.SeededResourceSubject.NEGOTIATION;
import static eu.bbmri_eric.negotiator.characterization.service.SeededResourceSubject.REPRESENTATIVE;
import static eu.bbmri_eric.negotiator.characterization.service.SeededResourceSubject.RESOURCE;
import static eu.bbmri_eric.negotiator.characterization.service.SeededResourceSubject.RESOURCE_ROW_ID;
import static eu.bbmri_eric.negotiator.characterization.service.SeededResourceSubject.authenticateAs;
import static eu.bbmri_eric.negotiator.characterization.service.SeededResourceSubject.linkResource;
import static eu.bbmri_eric.negotiator.characterization.service.SeededResourceSubject.putNegotiationInState;
import static eu.bbmri_eric.negotiator.characterization.service.SeededResourceSubject.putResourceInState;
import static eu.bbmri_eric.negotiator.characterization.service.StateChangeEvents.negotiationChanges;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.bbmri_eric.negotiator.characterization.adapter.LifecycleTestAdapter;
import eu.bbmri_eric.negotiator.characterization.adapter.LifecycleTestAdapterConfig;
import eu.bbmri_eric.negotiator.characterization.service.LifecycleHistory.NegotiationRecord;
import eu.bbmri_eric.negotiator.characterization.service.StateChangeEvents.NegotiationStateChange;
import eu.bbmri_eric.negotiator.util.IntegrationTest;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

/**
 * Pins automatic conclusion: a Negotiation whose Resources have all finished concludes itself,
 * without anybody firing {@code CONCLUDE}. ADR 0007 rebuilds this as a terminal aggregation Guard,
 * and the whole value of this class is that the Guard is then configured against what the system
 * does rather than against what the State names suggest.
 *
 * <p><b>The predicate is narrower than the names.</b> Twelve States are declared, four of them read
 * like ends of the road, and exactly two of them count: delivered, and unavailable. {@code
 * RESOURCE_NOT_MADE_AVAILABLE} - the State a researcher's own refusal of the access conditions
 * leads to - does not count, and neither does {@code RESOURCE_UNAVAILABLE_WILLING_TO_COLLECT}. A
 * Negotiation all of whose Resources ended in either therefore stays open for ever with nothing
 * left to do. That is pinned as behaviour, not endorsed.
 *
 * <p><b>The walk is done twice, over two different universes, because neither producer can cover
 * the whole of it.</b> The Lifecycle walk drives the subject Resource through every Transition of
 * the graph and asks after each one whether the Negotiation concluded - that is the predicate as a
 * user reaches it. The override walk writes each of the twelve declared States straight onto the
 * link row, which is the only way to reach the two States no Transition leads to at all, and is
 * itself a reachable admin-facing path. Both universes are computed from {@link ResourceGraphV1},
 * which is bound to the committed mechanical dump.
 *
 * <p><b>Conclusion is somebody else's action.</b> It runs through the system user, so the audit row
 * it writes is attributed to Person 0 rather than to the representative whose Event set it off. The
 * new subsystem has to reproduce that, because the Negotiation's own history is what a researcher
 * reads.
 *
 * <p><b>The listener carries two annotations for the same method</b> - a plain event listener and a
 * transactional one - which the parent PRD lists as pinned-not-fixed. It turns out to run once;
 * that is asserted below through the number of conclusions a single terminal Resource change
 * produces.
 *
 * <p><b>Asynchrony.</b> The listener runs after commit, in a new transaction, so every read is
 * bounded-polled through {@link LifecyclePersistence} and every "it did not conclude" settles
 * first.
 *
 * <p>{@code @DirtiesContext} per method restores the seed. This class concludes a shared
 * Negotiation that other classes expect to find running.
 */
@IntegrationTest(loadTestData = true)
@Import(LifecycleTestAdapterConfig.class)
@RecordApplicationEvents
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class NegotiationConclusionTest {

  /**
   * The two Resource States that count toward concluding a Negotiation, established by firing every
   * other declared State at the same predicate and watching nothing happen.
   */
  private static final Set<String> COUNTS_TOWARD_CONCLUSION =
      Set.of("RESOURCE_MADE_AVAILABLE", "RESOURCE_UNAVAILABLE");

  /** The State a Negotiation concludes into, and the one it sits in until it does. */
  private static final String CONCLUDED = "CONCLUDED";

  private static final String RUNNING = ResourceGraphV1.REQUIRED_PARENT_STATE;

  /**
   * The Person the system acts as when it takes an action nobody asked for. Seeded by the {@code
   * add_system_user} migration rather than by the test data, and not an administrator by the {@code
   * admin} column - only by the role the system context hands itself.
   */
  private static final long SYSTEM_USER = 0L;

  /** Long enough for the after-commit listener to have concluded had it been going to. */
  private static final Duration SETTLE = Duration.ofSeconds(3);

  /** Which seeded caller satisfies which of the Resource graph's three Required Authority rules. */
  private static final Map<String, Long> CALLER_SATISFYING =
      Map.of(
          ResourceGraphV1.IS_ADMIN, ADMIN,
          ResourceGraphV1.IS_REPRESENTATIVE, REPRESENTATIVE,
          ResourceGraphV1.IS_CREATOR, CREATOR);

  @Autowired LifecycleTestAdapter adapter;
  @Autowired JdbcTemplate jdbcTemplate;
  @Autowired ApplicationEvents events;

  @AfterEach
  void clearAuthentication() {
    SecurityContextHolder.clearContext();
  }

  private String negotiationState() {
    return adapter.currentNegotiationState(NEGOTIATION);
  }

  /** Drives the subject Resource along one Transition of the graph, as a caller who may. */
  private void drive(String source, String event) {
    putNegotiationInState(jdbcTemplate, RUNNING);
    putResourceInState(jdbcTemplate, source);
    authenticateAs(CALLER_SATISFYING.get(requiredAuthorityOf(source, event)));
    assertTrue(
        adapter.possibleResourceEvents(NEGOTIATION, RESOURCE).contains(event),
        "'%s' is not offered from '%s'".formatted(event, source));
    adapter.sendResourceEvent(NEGOTIATION, RESOURCE, event);
    LifecyclePersistence.awaitState(
        ResourceGraphV1.target(source, event),
        () -> adapter.currentResourceState(NEGOTIATION, RESOURCE));
  }

  private static String requiredAuthorityOf(String source, String event) {
    return ResourceGraphV1.TRANSITIONS.stream()
        .filter(edge -> edge.source().equals(source) && edge.event().equals(event))
        .map(ResourceGraphV1.Edge::requiredAuthority)
        .findFirst()
        .orElseThrow();
  }

  @Test
  @DisplayName("driving the only Resource to delivered concludes the Negotiation")
  void everyResourceDelivered_concludesTheNegotiation() {
    drive("ACCESS_CONDITIONS_MET", "GRANT_ACCESS_TO_RESOURCE");

    LifecyclePersistence.awaitState(CONCLUDED, this::negotiationState);
  }

  @Test
  @DisplayName("driving the only Resource to unavailable concludes the Negotiation")
  void everyResourceUnavailable_concludesTheNegotiation() {
    drive("REPRESENTATIVE_CONTACTED", "STEP_AWAY");

    LifecyclePersistence.awaitState(CONCLUDED, this::negotiationState);
  }

  /**
   * The predicate aggregates over Resources rather than requiring them to agree: one delivered and
   * one unavailable is enough.
   *
   * <p>The second Resource is put in its State by SQL rather than driven, so that the change that
   * sets the listener off is unambiguously the first Resource's.
   */
  @Test
  @DisplayName("a mix of delivered and unavailable Resources concludes the Negotiation")
  void aMixOfDeliveredAndUnavailable_concludesTheNegotiation() {
    linkResource(jdbcTemplate, ANOTHER_RESOURCE_ROW_ID, "RESOURCE_UNAVAILABLE");

    drive("ACCESS_CONDITIONS_MET", "GRANT_ACCESS_TO_RESOURCE");

    LifecyclePersistence.awaitState(CONCLUDED, this::negotiationState);
  }

  @Test
  @DisplayName(
      "one Resource still in play keeps the Negotiation open however finished the others are")
  void aNonTerminalResource_blocksConclusion() {
    linkResource(jdbcTemplate, ANOTHER_RESOURCE_ROW_ID, "CHECKING_AVAILABILITY");

    drive("ACCESS_CONDITIONS_MET", "GRANT_ACCESS_TO_RESOURCE");

    LifecyclePersistence.awaitStateAfterSettling(SETTLE, RUNNING, this::negotiationState);
  }

  /**
   * The predicate walked against every State a Transition of the graph leads to, one Transition at
   * a time. This is the version of the walk a user can actually reach.
   */
  @Test
  @DisplayName("only delivered and unavailable conclude, walked over every Transition of the graph")
  void theConclusionPredicate_walkedOverEveryTransition() {
    for (ResourceGraphV1.Edge edge : ResourceGraphV1.TRANSITIONS) {
      drive(edge.source(), edge.event());

      if (COUNTS_TOWARD_CONCLUSION.contains(edge.target())) {
        LifecyclePersistence.awaitValue(
            CONCLUDED,
            this::negotiationState,
            "'%s' counts toward conclusion, so arriving there must conclude"
                .formatted(edge.target()));
      } else {
        LifecyclePersistence.awaitValueAfterSettling(
            SETTLE,
            RUNNING,
            this::negotiationState,
            "'%s' does not count toward conclusion, so arriving there must not conclude"
                .formatted(edge.target()));
      }
    }
  }

  /**
   * The same predicate walked against all twelve declared States, through the override path.
   *
   * <p>This is what covers the two States the graph has no Transition into - the initial State,
   * which nothing leads back to, and the Legacy State no Transition names at all - so the answer
   * "does this State conclude a Negotiation" exists for every State ADR 0009's seed has to carry,
   * not only for the ones a Transition can reach.
   *
   * <p>The initial State is the one arm where nothing is written and nothing is published: the
   * override path treats writing it onto a link row that already has a State as no change at all.
   * The Negotiation staying open there says nothing about the predicate, and the assertion says so.
   */
  @Test
  @DisplayName("only delivered and unavailable conclude, walked over every declared State")
  void theConclusionPredicate_walkedOverEveryDeclaredState() {
    String startingPoint = "CHECKING_AVAILABILITY";
    authenticateAs(ADMIN);
    for (String state : ResourceGraphV1.allStateNames().stream().sorted().toList()) {
      putNegotiationInState(jdbcTemplate, RUNNING);
      putResourceInState(jdbcTemplate, startingPoint);

      adapter.overrideResourceStates(NEGOTIATION, List.of(RESOURCE_ROW_ID), state);

      if (state.equals(ResourceGraphV1.INITIAL_STATE)) {
        LifecyclePersistence.awaitValueAfterSettling(
            SETTLE,
            startingPoint,
            () -> adapter.currentResourceState(NEGOTIATION, RESOURCE),
            "an override to the initial State of a Resource that has one writes nothing");
        assertEquals(
            RUNNING, negotiationState(), "and having written nothing it can conclude nothing");
        continue;
      }

      LifecyclePersistence.awaitState(
          state, () -> adapter.currentResourceState(NEGOTIATION, RESOURCE));
      if (COUNTS_TOWARD_CONCLUSION.contains(state)) {
        LifecyclePersistence.awaitValue(
            CONCLUDED, this::negotiationState, "'%s' counts toward conclusion".formatted(state));
      } else {
        LifecyclePersistence.awaitValueAfterSettling(
            SETTLE,
            RUNNING,
            this::negotiationState,
            "'%s' does not count toward conclusion".formatted(state));
      }
    }
  }

  /**
   * The two States the parent ticket calls out by name, stated as a fact about the predicate rather
   * than left to be read out of a walk.
   *
   * <p>Both are places a Resource comes to rest and neither counts, so a Negotiation can reach a
   * position where no Resource has anything left to do and the Negotiation is still running.
   */
  @Test
  @DisplayName(
      "the two other unavailable-sounding States are declared, terminal-looking, and do not count")
  void theUnavailableSoundingStates_doNotCount() {
    Set<String> unavailableSounding =
        Set.of("RESOURCE_NOT_MADE_AVAILABLE", "RESOURCE_UNAVAILABLE_WILLING_TO_COLLECT");

    assertThat(ResourceGraphV1.allStateNames()).containsAll(unavailableSounding);
    assertThat(COUNTS_TOWARD_CONCLUSION).doesNotContainAnyElementsOf(unavailableSounding);
    assertThat(COUNTS_TOWARD_CONCLUSION)
        .as("and the predicate is a strict subset of the declared States")
        .isSubsetOf(ResourceGraphV1.allStateNames())
        .hasSizeLessThan(ResourceGraphV1.allStateNames().size());

    linkResource(jdbcTemplate, ANOTHER_RESOURCE_ROW_ID, "RESOURCE_NOT_MADE_AVAILABLE");
    drive("CHECKING_AVAILABILITY", "MARK_AS_CURRENTLY_UNAVAILABLE_BUT_WILLING_TO_COLLECT");

    LifecyclePersistence.awaitValueAfterSettling(
        SETTLE,
        RUNNING,
        this::negotiationState,
        "a Negotiation whose Resources are all in one of those two States never concludes");
  }

  /**
   * Conclusion is attributed to the system user, not to whoever's Event triggered it.
   *
   * <p>The trail is the Negotiation's own history and a researcher reads it, so the difference
   * between "the biobanker concluded your request" and "the system concluded your request" is
   * user-visible. Read through {@link LifecycleHistory} so that ADR 0008's conversion of the State
   * column does not touch this assertion.
   */
  @Test
  @DisplayName("conclusion is recorded as the system user's doing, not the caller's")
  void conclusion_isPerformedAsTheSystemUser() {
    List<NegotiationRecord> before = negotiationRecordsOf(jdbcTemplate, NEGOTIATION);

    drive("ACCESS_CONDITIONS_MET", "GRANT_ACCESS_TO_RESOURCE");
    LifecyclePersistence.awaitState(CONCLUDED, this::negotiationState);
    LifecyclePersistence.awaitValue(
        before.size() + 1, () -> negotiationRecordsOf(jdbcTemplate, NEGOTIATION).size());

    List<NegotiationRecord> after = negotiationRecordsOf(jdbcTemplate, NEGOTIATION);
    NegotiationRecord written = after.get(after.size() - 1);
    assertEquals(CONCLUDED, written.changedTo());
    assertEquals(
        SYSTEM_USER, written.createdBy().longValue(), "the conclusion is the system user's row");
    assertNotEquals(
        REPRESENTATIVE,
        written.createdBy().longValue(),
        "and not the caller's, whose Event is what set it off");
  }

  /**
   * The double-annotated listener, answered by counting.
   *
   * <p>One Resource reaching a counting State produces exactly one conclusion: one published
   * Negotiation state change and one row on the trail. Were both annotations producing a listener,
   * the second invocation would either duplicate the row or refuse - and a refusal escaping an
   * after-commit listener is not something a caller would fail to notice. Nothing here fixes the
   * duplication; it records that today it costs nothing.
   */
  @Test
  @DisplayName("a single terminal Resource change concludes the Negotiation exactly once")
  void conclusion_happensExactlyOnce() {
    int before = negotiationRecordsOf(jdbcTemplate, NEGOTIATION).size();

    drive("ACCESS_CONDITIONS_MET", "GRANT_ACCESS_TO_RESOURCE");
    LifecyclePersistence.awaitState(CONCLUDED, this::negotiationState);

    LifecyclePersistence.awaitValueAfterSettling(
        SETTLE,
        List.of(new NegotiationStateChange(NEGOTIATION, RUNNING, CONCLUDED, "CONCLUDE")),
        () -> negotiationChanges(events),
        "exactly one conclusion was published");
    assertEquals(
        List.of(CONCLUDED),
        negotiationRecordsOf(jdbcTemplate, NEGOTIATION).stream()
            .skip(before)
            .map(NegotiationRecord::changedTo)
            .collect(Collectors.toList()),
        "and exactly one row was added to the trail");
  }
}
