package eu.bbmri_eric.negotiator.characterization.service;

import static eu.bbmri_eric.negotiator.characterization.service.SeededNegotiationSubject.NO_RESOURCES;
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
import static eu.bbmri_eric.negotiator.characterization.service.StateChangeEvents.resourceChanges;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.bbmri_eric.negotiator.characterization.adapter.LifecycleTestAdapter;
import eu.bbmri_eric.negotiator.characterization.adapter.LifecycleTestAdapterConfig;
import eu.bbmri_eric.negotiator.characterization.service.StateChangeEvents.NegotiationStateChange;
import eu.bbmri_eric.negotiator.characterization.service.StateChangeEvents.ResourceStateChange;
import eu.bbmri_eric.negotiator.common.exceptions.ForbiddenRequestException;
import eu.bbmri_eric.negotiator.util.IntegrationTest;
import java.time.Duration;
import java.util.List;
import java.util.Map;
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
 * Pins the payload of the two state change application events, which every notification
 * handler, the automatic conclusion listener and the webhook subsystem read.
 *
 * <p>Everything that happens because of a Transition, rather than in it, listens to these two
 * events, and no registry lists the listeners.
 *
 * <p>{@link StateChangeEvents} reads each payload as names. The expectations come from {@link
 * NegotiationGraphV1} and {@link ResourceGraphV1}, both bound edge for edge to the committed dump,
 * so the walks cover every Transition.
 *
 * <p>A Negotiation state change always comes from a Transition. A Resource state change has a
 * second producer: the Override writes any State onto the link row and publishes the same event
 * with Event {@code OVERRIDE}, without checking the graph. Both producers are pinned separately,
 * because code that assumed the event implies a Transition would be wrong for a reachable admin
 * path. Spawn also writes Resource States but publishes no event; {@link NegotiationSpawnTest} pins
 * that.
 *
 * <p>Both events are published on the calling thread before {@code sendEvent} returns. The State
 * they announce is written on the same path, so the tests wait for it through {@link
 * LifecyclePersistence}. Checks that no event was published wait a settle period first.
 *
 * <p>{@code @DirtiesContext} per method restores the seed: this class moves shared Negotiations and
 * a shared Resource.
 */
@IntegrationTest(loadTestData = true)
@Import(LifecycleTestAdapterConfig.class)
@RecordApplicationEvents
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class LifecycleStateChangeEventsTest {

  /** Long enough for an event to have been published, if one were coming. */
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

  /**
   * For all eight Negotiation Transitions, firing an Event publishes exactly one state change naming
   * the Negotiation, the State left, the State arrived in and the Event.
   *
   * <p>Uses {@code negotiation-2}, which has no Resources, so arriving at {@code IN_PROGRESS} adds no
   * events of its own.
   */
  @Test
  @DisplayName(
      "every Negotiation Transition publishes one state change naming both States and the Event")
  void everyNegotiationTransition_publishesItsOwnStateChange() {
    authenticateAs(ADMIN);
    int published = 0;
    for (NegotiationGraphV1.Edge edge : NegotiationGraphV1.TRANSITIONS) {
      SeededNegotiationSubject.putInState(jdbcTemplate, NO_RESOURCES, edge.source());
      assertTrue(
          adapter.possibleNegotiationEvents(NO_RESOURCES).contains(edge.event()),
          "'%s' is not offered from '%s', so nothing about its event can be observed"
              .formatted(edge.event(), edge.source()));

      adapter.sendNegotiationEvent(NO_RESOURCES, edge.event());
      LifecyclePersistence.awaitState(
          edge.target(), () -> adapter.currentNegotiationState(NO_RESOURCES));

      published++;
      List<NegotiationStateChange> changes = negotiationChanges(events);
      assertEquals(
          published,
          changes.size(),
          "a Transition publishes exactly one state change, no more and no fewer");
      assertEquals(
          new NegotiationStateChange(NO_RESOURCES, edge.source(), edge.target(), edge.event()),
          changes.get(changes.size() - 1));
    }
  }

  /**
   * For all thirteen Resource Transitions, the same, plus the Resource's {@code source_id}.
   *
   * <p>A second Resource sits in a State the conclusion check does not count, so moving the subject
   * into a terminal State cannot conclude the Negotiation before the next arm. {@link
   * NegotiationConclusionTest} covers conclusion.
   */
  @Test
  @DisplayName(
      "every Resource Transition publishes one state change naming the Resource, both States and the Event")
  void everyResourceTransition_publishesItsOwnStateChange() {
    linkResource(jdbcTemplate, ANOTHER_RESOURCE_ROW_ID, "CHECKING_AVAILABILITY");
    int published = 0;
    for (ResourceGraphV1.Edge edge : ResourceGraphV1.TRANSITIONS) {
      putNegotiationInState(jdbcTemplate, ResourceGraphV1.REQUIRED_PARENT_STATE);
      putResourceInState(jdbcTemplate, edge.source());
      authenticateAs(CALLER_SATISFYING.get(edge.requiredAuthority()));
      assertTrue(
          adapter.possibleResourceEvents(NEGOTIATION, RESOURCE).contains(edge.event()),
          "'%s' is not offered from '%s', so nothing about its event can be observed"
              .formatted(edge.event(), edge.source()));

      adapter.sendResourceEvent(NEGOTIATION, RESOURCE, edge.event());
      LifecyclePersistence.awaitState(
          edge.target(), () -> adapter.currentResourceState(NEGOTIATION, RESOURCE));

      published++;
      List<ResourceStateChange> changes = resourceChanges(events);
      assertEquals(
          published,
          changes.size(),
          "a Transition publishes exactly one state change, no more and no fewer");
      assertEquals(
          new ResourceStateChange(
              NEGOTIATION, RESOURCE, edge.source(), edge.target(), edge.event()),
          changes.get(changes.size() - 1));
    }
  }

  /**
   * The Override as a producer.
   *
   * <p>No Transition leads from the Resource's State to the State written here, and the payload's
   * Event has no Transition anywhere. A consumer reading it as "the Resource did {@code OVERRIDE}
   * from {@code SUBMITTED}" would describe a Transition that does not exist. It still reaches every
   * listener and handler a Transition reaches.
   */
  @Test
  @DisplayName(
      "the governance override publishes the same event, stamped OVERRIDE, with no Transition behind it")
  void overrideProducer_publishesAStateChangeThatTracesNoTransition() {
    String target = "ACCESS_CONDITIONS_MET";
    authenticateAs(ADMIN);
    putResourceInState(jdbcTemplate, ResourceGraphV1.INITIAL_STATE);
    assertThat(ResourceGraphV1.TRANSITIONS)
        .as("no Transition leads there from the initial State")
        .noneMatch(
            edge ->
                edge.source().equals(ResourceGraphV1.INITIAL_STATE)
                    && edge.target().equals(target));

    adapter.overrideResourceStates(NEGOTIATION, List.of(RESOURCE_ROW_ID), target);

    LifecyclePersistence.awaitState(
        target, () -> adapter.currentResourceState(NEGOTIATION, RESOURCE));
    assertEquals(
        List.of(
            new ResourceStateChange(
                NEGOTIATION, RESOURCE, ResourceGraphV1.INITIAL_STATE, target, "OVERRIDE")),
        resourceChanges(events));
    assertThat(ResourceGraphV1.EVENTS_ON_NO_TRANSITION)
        .as("no Transition anywhere is triggered by the payload's Event")
        .contains("OVERRIDE");
  }

  /**
   * The one State the override path refuses to announce.
   *
   * <p>Writing the graph's initial State onto a link row that already has a State is the one case
   * the governance service treats as no change: it writes and publishes nothing. An Override that
   * handled this case like any other would publish an event and could conclude Negotiations.
   */
  @Test
  @DisplayName(
      "an override to the initial State of a Resource that has one writes nothing and publishes nothing")
  void overrideProducer_toTheInitialState_isSilent() {
    authenticateAs(ADMIN);
    putResourceInState(jdbcTemplate, "CHECKING_AVAILABILITY");

    adapter.overrideResourceStates(
        NEGOTIATION, List.of(RESOURCE_ROW_ID), ResourceGraphV1.INITIAL_STATE);

    LifecyclePersistence.awaitValueAfterSettling(
        SETTLE, "CHECKING_AVAILABILITY", () -> adapter.currentResourceState(NEGOTIATION, RESOURCE));
    assertEquals(List.of(), resourceChanges(events));
  }

  /**
   * A refusal is not a state change, on either side.
   *
   * <p>Both services refuse by throwing, and neither publishes anything.
   */
  @Test
  @DisplayName("neither service publishes a state change for an Event it refuses")
  void aRefusedEvent_publishesNoStateChange() {
    authenticateAs(CREATOR);
    SeededNegotiationSubject.putInState(jdbcTemplate, NO_RESOURCES, "SUBMITTED");
    assertThrows(
        ForbiddenRequestException.class,
        () -> adapter.sendNegotiationEvent(NO_RESOURCES, "APPROVE"),
        "the Negotiation service refuses an Event the caller is not offered");

    putNegotiationInState(jdbcTemplate, ResourceGraphV1.REQUIRED_PARENT_STATE);
    putResourceInState(jdbcTemplate, ResourceGraphV1.INITIAL_STATE);
    assertThrows(
        ForbiddenRequestException.class,
        () -> adapter.sendResourceEvent(NEGOTIATION, RESOURCE, "CONTACT"),
        "the Resource service refuses an Event the caller has no Required Authority for");

    LifecyclePersistence.awaitValueAfterSettling(
        SETTLE, List.of(), () -> negotiationChanges(events));
    assertEquals(List.of(), resourceChanges(events));
  }
}
