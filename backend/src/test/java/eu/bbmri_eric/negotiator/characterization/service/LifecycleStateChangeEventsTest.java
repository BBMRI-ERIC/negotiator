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
 * Pins the payload of the two state change application events - the seam every notification
 * handler, the automatic conclusion listener and the webhook subsystem read.
 *
 * <p>Everything that happens <em>because</em> of a Transition rather than <em>in</em> it hangs off
 * these two events, so what they carry is a contract in the strict sense: five separate subsystems
 * consume it and none of them is in a registry anybody can enumerate. The parent PRD calls the enum
 * removal's effect here the likeliest silent breakage in the whole rewrite.
 *
 * <p><b>Both payloads are read as names.</b> Origin State, destination State and triggering Event
 * are enum constants today and are gone at cutover; {@link StateChangeEvents} converts each payload
 * to strings once so that every assertion below survives. The expectations themselves are computed
 * from {@link NegotiationGraphV1} and {@link ResourceGraphV1}, both of which are bound edge for
 * edge to the committed mechanical dump, so the walks are complete rather than as complete as
 * somebody remembered.
 *
 * <p><b>A Negotiation state change has one producer; a Resource state change has two.</b> Every
 * Negotiation state change is the trace of a Transition. A Resource state change is not: the
 * governance service writes an arbitrary State straight onto the link row and publishes the same
 * event stamped {@code OVERRIDE}, having consulted no Definition graph at all. Both producers are
 * pinned here, separately and by name, because a reimplementation that assumed the event implies a
 * Transition would be wrong for an entire, reachable, admin-facing path. A third writer of Resource
 * States - spawn - publishes no event whatsoever, which {@link NegotiationSpawnTest} pins.
 *
 * <p><b>Asynchrony.</b> Both events turn out to be published on the calling thread before {@code
 * sendEvent} returns, which is why they are recordable at all; the State they announce is written
 * on the same path and is awaited through {@link LifecyclePersistence} before anything is read
 * back. Claims that no event was published settle first.
 *
 * <p>{@code @DirtiesContext} per method restores the seed: this class moves shared Negotiations and
 * a shared Resource.
 */
@IntegrationTest(loadTestData = true)
@Import(LifecycleTestAdapterConfig.class)
@RecordApplicationEvents
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class LifecycleStateChangeEventsTest {

  /** Long enough for an event to have been published had one been going to be. */
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
   * The Negotiation half of the contract, made for all eight Transitions: firing an Event publishes
   * exactly one state change, and it names the Negotiation, the State left, the State arrived in
   * and the Event that did it.
   *
   * <p>Carried on {@code negotiation-2}, which has no Resources, so nothing the arrival at {@code
   * IN_PROGRESS} sets off can add events of its own to what is being counted.
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
   * The Resource half, made for all thirteen Transitions, and one field wider: a Resource state
   * change names the Resource as well.
   *
   * <p>The Resource is named by its {@code source_id}, which is what the whole Lifecycle keys on -
   * see {@link SeededResourceSubject}. A second Resource is attached in a State the conclusion
   * predicate does not count, purely so that driving the subject into a terminal State cannot
   * conclude the Negotiation out from under the next arm of the walk; what conclusion does and does
   * not do is {@link NegotiationConclusionTest}'s subject, not this one's.
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
   * The second producer, pinned as what it is.
   *
   * <p>The State written here is one no Transition leads to from the State the Resource was in, and
   * the Event stamped on the payload is one that carries no Transition anywhere in the graph. So
   * this event is not the trace of anything the Definition describes, and a consumer that read it
   * as one - "the Resource just did {@code OVERRIDE} from {@code SUBMITTED}" - would be describing
   * a Transition that does not exist. It reaches every listener and every handler the Lifecycle
   * producer reaches, which is what makes it worth recording rather than scoping out.
   */
  @Test
  @DisplayName(
      "the governance override publishes the same event, stamped OVERRIDE, with no Transition behind it")
  void overrideProducer_publishesAStateChangeThatTracesNoTransition() {
    String target = "ACCESS_CONDITIONS_MET";
    authenticateAs(ADMIN);
    putResourceInState(jdbcTemplate, ResourceGraphV1.INITIAL_STATE);
    assertThat(ResourceGraphV1.TRANSITIONS)
        .as("the point of this test is that no Transition leads there from the initial State")
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
        .as("the Event the payload is stamped with is one no Transition anywhere is triggered by")
        .contains("OVERRIDE");
  }

  /**
   * The one State the override path refuses to announce.
   *
   * <p>Writing the graph's initial State onto a link row that already has a State is the single
   * case the governance service treats as "not a change": it writes nothing and publishes nothing,
   * so no listener and no handler learns of it. A reimplementation that made the override uniform
   * would start publishing an event this path has never published, and conclude Negotiations this
   * path has never concluded.
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
   * <p>Worth one test because the two services refuse so differently - the Negotiation raises, the
   * Resource returns its unchanged State and says nothing - and a consumer downstream of the event
   * seam must not be able to tell them apart, because neither produced anything.
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
    assertEquals(
        ResourceGraphV1.INITIAL_STATE,
        adapter.sendResourceEvent(NEGOTIATION, RESOURCE, "CONTACT"),
        "the Resource service refuses silently, returning the State it did not leave");

    LifecyclePersistence.awaitValueAfterSettling(
        SETTLE, List.of(), () -> negotiationChanges(events));
    assertEquals(List.of(), resourceChanges(events));
  }
}
