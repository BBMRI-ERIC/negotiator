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
 * without anybody firing {@code CONCLUDE}. A replacement must match what the system does, not what
 * the State names suggest.
 *
 * <p>Twelve States are declared, four of them sound final, and only two count: delivered and
 * unavailable. {@code RESOURCE_NOT_MADE_AVAILABLE}, where a researcher's refusal of the access
 * conditions leads, does not count, and neither does {@code
 * RESOURCE_UNAVAILABLE_WILLING_TO_COLLECT}. A Negotiation whose Resources all end in either stays
 * open for ever with nothing left to do. That is pinned as behaviour, not endorsed.
 *
 * <p>The walk runs twice. The Lifecycle walk moves the subject Resource through every Transition
 * and checks after each whether the Negotiation concluded, as a user would reach it. The Override
 * walk writes each of the twelve declared States onto the link row, the only way to reach the two
 * States no Transition leads to, and itself a path admins can use. Both take their States from
 * {@link ResourceGraphV1}, which is bound to the committed dump.
 *
 * <p>Conclusion runs as the system user, so its Lifecycle Record is attributed to Person 0, not to
 * the representative whose Event set it off. Researchers read that history, so a replacement must
 * keep it.
 *
 * <p>The listener method has two annotations, a plain event listener and a transactional one. It
 * still runs once; a test below counts the conclusions one terminal Resource change produces.
 *
 * <p>The listener runs after commit in a new transaction, so every read polls through {@link
 * LifecyclePersistence} and every "it did not conclude" check waits a settle period first.
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
   * The two Resource States that count toward concluding a Negotiation. The walks below show every
   * other declared State does not.
   */
  private static final Set<String> COUNTS_TOWARD_CONCLUSION =
      Set.of("RESOURCE_MADE_AVAILABLE", "RESOURCE_UNAVAILABLE");

  /** The State a Negotiation concludes into, and the one it sits in until it does. */
  private static final String CONCLUDED = "CONCLUDED";

  private static final String RUNNING = ResourceGraphV1.REQUIRED_PARENT_STATE;

  /**
   * The Person the system acts as. The {@code add_system_user} migration seeds it, not the test data,
   * and its {@code admin} column is false; it is an administrator only through the role the system
   * context grants itself.
   */
  private static final long SYSTEM_USER = 0L;

  /** Long enough for the after-commit listener to have concluded, if it were going to. */
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
   * The Resources need not agree: one delivered and one unavailable is enough.
   *
   * <p>The second Resource gets its State through SQL, so only the first Resource's change can set
   * the listener off.
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
   * The check walked over every State a Transition leads to, one Transition at a time, as a user
   * can reach it.
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
   * The same check walked over all twelve declared States, through the Override.
   *
   * <p>This covers the two States no Transition leads into, the initial State and the Legacy State,
   * so every declared State has an answer.
   *
   * <p>For the initial State the Override writes and publishes nothing, because it treats writing
   * it onto a link row that already has a State as no change. The Negotiation staying open there
   * says nothing about the check, and the assertion message says so.
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
   * The two States that sound final but do not count, stated directly rather than left to the walk.
   *
   * <p>A Resource stops in either, so a Negotiation can have no Resource with anything left to do
   * and still be running.
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
   * <p>Researchers read the Negotiation's history, so "the biobanker concluded your request" versus
   * "the system concluded your request" is visible to them. Read through {@link LifecycleHistory}
   * so a change to how the State column is stored does not touch this assertion.
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
   * The listener with two annotations runs once.
   *
   * <p>One Resource reaching a counting State produces one published Negotiation state change and
   * one Lifecycle Record. If both annotations registered a listener, the second call would duplicate
   * the row or throw. The double annotation stays; this shows it has no effect.
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
