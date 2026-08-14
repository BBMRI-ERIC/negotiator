package eu.bbmri_eric.negotiator.characterization.service;

import static eu.bbmri_eric.negotiator.characterization.service.HandlerNotifications.LifecycleHandler.NEGOTIATION_IN_PROGRESS;
import static eu.bbmri_eric.negotiator.characterization.service.HandlerNotifications.LifecycleHandler.RESOURCE_STATE_CHANGE;
import static eu.bbmri_eric.negotiator.characterization.service.HandlerNotifications.recipientsOf;
import static eu.bbmri_eric.negotiator.characterization.service.HandlerNotifications.writtenBy;
import static eu.bbmri_eric.negotiator.characterization.service.SeededNegotiationSubject.ADMIN;
import static eu.bbmri_eric.negotiator.characterization.service.SeededNegotiationSubject.FIRST_RESOURCE;
import static eu.bbmri_eric.negotiator.characterization.service.SeededNegotiationSubject.REPRESENTATIVE_OF_FIRST_RESOURCE;
import static eu.bbmri_eric.negotiator.characterization.service.SeededNegotiationSubject.REPRESENTATIVE_OF_SECOND_RESOURCE;
import static eu.bbmri_eric.negotiator.characterization.service.SeededNegotiationSubject.SECOND_RESOURCE;
import static eu.bbmri_eric.negotiator.characterization.service.SeededNegotiationSubject.UNREPRESENTED_RESOURCE;
import static eu.bbmri_eric.negotiator.characterization.service.SeededNegotiationSubject.UNREPRESENTED_RESOURCE_ROW_ID;
import static eu.bbmri_eric.negotiator.characterization.service.SeededNegotiationSubject.WITH_UNINITIALISED_RESOURCES;
import static eu.bbmri_eric.negotiator.characterization.service.SeededNegotiationSubject.clearResourceStates;
import static eu.bbmri_eric.negotiator.characterization.service.SeededNegotiationSubject.linkResource;
import static eu.bbmri_eric.negotiator.characterization.service.SeededNegotiationSubject.putInState;
import static eu.bbmri_eric.negotiator.characterization.service.SeededNegotiationSubject.resourceStates;
import static eu.bbmri_eric.negotiator.characterization.service.SeededResourceSubject.authenticateAs;
import static eu.bbmri_eric.negotiator.characterization.service.StateChangeEvents.resourceChanges;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.bbmri_eric.negotiator.characterization.adapter.LifecycleTestAdapter;
import eu.bbmri_eric.negotiator.characterization.adapter.LifecycleTestAdapterConfig;
import eu.bbmri_eric.negotiator.util.IntegrationTest;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
 * Pins spawn: what a Negotiation's arrival at {@code IN_PROGRESS} does to the Resources attached to
 * it, and who gets told. ADR 0007 moves this into a {@code SPAWN_RESOURCE_LIFECYCLES} Action, and
 * these are the statements the new Action has to keep true.
 *
 * <p><b>Three things here are not what the ticket, the ADR or the state names predict, and each is
 * asserted rather than assumed.</b>
 *
 * <ul>
 *   <li><b>Spawn does not start a Resource in the graph's initial State.</b> The Definition's
 *       initial State is {@code SUBMITTED}; spawn writes {@code REPRESENTATIVE_CONTACTED}, or
 *       {@code REPRESENTATIVE_UNREACHABLE} when the Resource has nobody to contact. So the State a
 *       Resource is seeded in and the State a Resource is spawned in are two different States, and
 *       a reimplementation that "started Resources at the initial State" would silently move every
 *       spawned Resource one step back and hand every representative an Event they have already
 *       been past.
 *   <li><b>Spawn keys on the destination State, not on the Event.</b> Every arrival at {@code
 *       IN_PROGRESS} spawns, including the one that comes back from {@code PAUSED}. Attaching the
 *       Action to {@code APPROVE} would lose that.
 *   <li><b>Spawn publishes no Resource state change.</b> It writes States straight onto the link
 *       rows, so none of the listeners downstream of that event - the automatic conclusion listener
 *       included - ever hears about the States it wrote. Every other writer of a Resource State
 *       publishes one. If ADR 0007's Action routes spawn through the ordinary Transition machinery
 *       it will start emitting events that have never been emitted, to consumers nobody has
 *       counted.
 * </ul>
 *
 * <p><b>Subject.</b> {@link SeededNegotiationSubject#WITH_UNINITIALISED_RESOURCES}, the only seeded
 * Negotiation still holding Resources with no State, plus the seed's only unrepresented Resource
 * attached by hand. Its two seeded Resources have two different representatives and a third
 * researcher created it, so "who was notified" has a discriminating answer.
 *
 * <p><b>Observation.</b> The States through the link rows, the notifications through the rows the
 * handler writes - never through SMTP - and the absence of an event through
 * {@code @RecordApplicationEvents}. Every read is bounded-polled through {@link
 * LifecyclePersistence}; every claim that nothing happened settles first.
 *
 * <p>{@code @DirtiesContext} per method restores the seed: this class moves a shared Negotiation
 * and writes States onto shared link rows.
 */
@IntegrationTest(loadTestData = true)
@Import(LifecycleTestAdapterConfig.class)
@RecordApplicationEvents
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class NegotiationSpawnTest {

  /** Long enough for spawn to have run had it been going to. */
  private static final Duration SETTLE = Duration.ofSeconds(3);

  /** What spawn writes for a Resource somebody represents. */
  private static final String CONTACTED = "REPRESENTATIVE_CONTACTED";

  /** What spawn writes for a Resource nobody represents. */
  private static final String UNREACHABLE = "REPRESENTATIVE_UNREACHABLE";

  @Autowired LifecycleTestAdapter adapter;
  @Autowired JdbcTemplate jdbcTemplate;
  @Autowired ApplicationEvents events;

  @BeforeEach
  void attachTheUnrepresentedResource() {
    linkResource(jdbcTemplate, WITH_UNINITIALISED_RESOURCES, UNREPRESENTED_RESOURCE_ROW_ID, null);
    authenticateAs(ADMIN);
  }

  @AfterEach
  void clearAuthentication() {
    SecurityContextHolder.clearContext();
  }

  private Map<String, String> statesOfTheSubjectsResources() {
    return resourceStates(jdbcTemplate, WITH_UNINITIALISED_RESOURCES);
  }

  private static Map<String, String> allUninitialised() {
    Map<String, String> states = new HashMap<>();
    states.put(FIRST_RESOURCE, null);
    states.put(SECOND_RESOURCE, null);
    states.put(UNREPRESENTED_RESOURCE, null);
    return states;
  }

  /**
   * The core statement: arriving at {@code IN_PROGRESS} gives every Resource that had no State one,
   * and which State depends on whether anybody can be told about it.
   */
  @Test
  @DisplayName(
      "reaching IN_PROGRESS gives every Resource without a State one, keyed on whether it has a representative")
  void reachingInProgress_initialisesEveryResourceThatHadNoState() {
    assertEquals(
        allUninitialised(), statesOfTheSubjectsResources(), "the subject starts unspawned");

    adapter.sendNegotiationEvent(WITH_UNINITIALISED_RESOURCES, "APPROVE");

    LifecyclePersistence.awaitValue(
        Map.of(
            FIRST_RESOURCE, CONTACTED,
            SECOND_RESOURCE, CONTACTED,
            UNREPRESENTED_RESOURCE, UNREACHABLE),
        this::statesOfTheSubjectsResources,
        "the States spawn writes");
  }

  /**
   * The divergence stated on its own, so that it reads as a finding rather than as a detail of the
   * expectation above.
   */
  @Test
  @DisplayName("the State spawn starts a Resource in is not the Definition's initial State")
  void spawn_doesNotUseTheGraphsInitialState() {
    adapter.sendNegotiationEvent(WITH_UNINITIALISED_RESOURCES, "APPROVE");
    LifecyclePersistence.awaitValue(
        CONTACTED, () -> statesOfTheSubjectsResources().get(FIRST_RESOURCE));

    assertNotEquals(
        ResourceGraphV1.INITIAL_STATE,
        statesOfTheSubjectsResources().get(FIRST_RESOURCE),
        "spawn and the Definition's initial State disagree, and the Definition is not what wins");
    assertThat(statesOfTheSubjectsResources().values())
        .as("neither of the two States spawn writes is the initial State")
        .doesNotContain(ResourceGraphV1.INITIAL_STATE);
    assertThat(ResourceGraphV1.allStateNames())
        .as("both are States the Definition declares, even though no Transition reaches them here")
        .contains(CONTACTED, UNREACHABLE);
  }

  /** The notification half of spawn: the representatives of the Resources it initialised, only. */
  @Test
  @DisplayName("reaching IN_PROGRESS notifies the representatives of the Resources it initialised")
  void reachingInProgress_notifiesTheRepresentatives() {
    adapter.sendNegotiationEvent(WITH_UNINITIALISED_RESOURCES, "APPROVE");

    LifecyclePersistence.awaitValue(
        Set.of(REPRESENTATIVE_OF_FIRST_RESOURCE, REPRESENTATIVE_OF_SECOND_RESOURCE),
        () -> recipientsOf(jdbcTemplate, NEGOTIATION_IN_PROGRESS, WITH_UNINITIALISED_RESOURCES),
        "the representatives spawn notified");
    assertEquals(
        2,
        writtenBy(jdbcTemplate, NEGOTIATION_IN_PROGRESS).size(),
        "one notification per representative, and none for the Resource nobody represents");
  }

  /**
   * Spawn is not a reset. A Resource that already has a State keeps it, and - the sharper half -
   * its representative is not notified either, because the two decisions are made together.
   */
  @Test
  @DisplayName(
      "spawn leaves a Resource that already has a State alone, and does not notify its representative")
  void spawn_skipsAResourceThatAlreadyHasAState() {
    SeededNegotiationSubject.putResourceInState(
        jdbcTemplate,
        WITH_UNINITIALISED_RESOURCES,
        SeededNegotiationSubject.FIRST_RESOURCE_ROW_ID,
        "CHECKING_AVAILABILITY");

    adapter.sendNegotiationEvent(WITH_UNINITIALISED_RESOURCES, "APPROVE");

    LifecyclePersistence.awaitValue(
        Map.of(
            FIRST_RESOURCE, "CHECKING_AVAILABILITY",
            SECOND_RESOURCE, CONTACTED,
            UNREPRESENTED_RESOURCE, UNREACHABLE),
        this::statesOfTheSubjectsResources,
        "spawn touches only the Resources that had no State");
    assertEquals(
        Set.of(REPRESENTATIVE_OF_SECOND_RESOURCE),
        recipientsOf(jdbcTemplate, NEGOTIATION_IN_PROGRESS, WITH_UNINITIALISED_RESOURCES),
        "the representative of the Resource spawn skipped is not told");
  }

  /**
   * Spawn keys on the State arrived in. Firing the other Transition that ends at {@code
   * IN_PROGRESS} produces the same spawn, which is what stops ADR 0007 from attaching the Action to
   * {@code APPROVE}.
   */
  @Test
  @DisplayName("every arrival at IN_PROGRESS spawns, not only the one from SUBMITTED")
  void unpausingIntoInProgress_spawnsTheSameWay() {
    putInState(jdbcTemplate, WITH_UNINITIALISED_RESOURCES, "PAUSED");

    adapter.sendNegotiationEvent(WITH_UNINITIALISED_RESOURCES, "UNPAUSE");

    LifecyclePersistence.awaitValue(
        Map.of(
            FIRST_RESOURCE, CONTACTED,
            SECOND_RESOURCE, CONTACTED,
            UNREPRESENTED_RESOURCE, UNREACHABLE),
        this::statesOfTheSubjectsResources,
        "the States an UNPAUSE spawns");
    assertEquals(
        Set.of(REPRESENTATIVE_OF_FIRST_RESOURCE, REPRESENTATIVE_OF_SECOND_RESOURCE),
        recipientsOf(jdbcTemplate, NEGOTIATION_IN_PROGRESS, WITH_UNINITIALISED_RESOURCES));
  }

  /**
   * The negative half of the same statement, walked over every Transition of the graph that ends
   * anywhere else. Computed from {@link NegotiationGraphV1}, so it covers the whole graph rather
   * than the destinations somebody thought of.
   */
  @Test
  @DisplayName("no Transition ending anywhere but IN_PROGRESS spawns anything or notifies anybody")
  void noOtherDestination_spawns() {
    for (NegotiationGraphV1.Edge edge : NegotiationGraphV1.TRANSITIONS) {
      if (edge.target().equals("IN_PROGRESS")) {
        continue;
      }
      HandlerNotifications.clear(jdbcTemplate);
      clearResourceStates(jdbcTemplate, WITH_UNINITIALISED_RESOURCES);
      putInState(jdbcTemplate, WITH_UNINITIALISED_RESOURCES, edge.source());
      assertTrue(
          adapter.possibleNegotiationEvents(WITH_UNINITIALISED_RESOURCES).contains(edge.event()),
          "'%s' is not offered from '%s'".formatted(edge.event(), edge.source()));

      adapter.sendNegotiationEvent(WITH_UNINITIALISED_RESOURCES, edge.event());

      LifecyclePersistence.awaitState(
          edge.target(), () -> adapter.currentNegotiationState(WITH_UNINITIALISED_RESOURCES));
      LifecyclePersistence.awaitValueAfterSettling(
          SETTLE,
          allUninitialised(),
          this::statesOfTheSubjectsResources,
          "arriving at '%s' must leave every Resource unspawned".formatted(edge.target()));
      assertEquals(
          List.of(),
          writtenBy(jdbcTemplate, NEGOTIATION_IN_PROGRESS),
          "arriving at '%s' must notify no representative".formatted(edge.target()));
    }
  }

  /**
   * The States spawn writes are invisible at the event seam.
   *
   * <p>Three Resources change State here and not one Resource state change is published, so the
   * handler that fires on every such event does not fire, and neither does the automatic conclusion
   * listener. That is the whole reason a Negotiation is never concluded by the act of spawning
   * Resources into an unavailable State.
   */
  @Test
  @DisplayName("spawn writes States without publishing any Resource state change")
  void spawn_publishesNoResourceStateChange() {
    adapter.sendNegotiationEvent(WITH_UNINITIALISED_RESOURCES, "APPROVE");

    LifecyclePersistence.awaitValue(
        Map.of(
            FIRST_RESOURCE, CONTACTED,
            SECOND_RESOURCE, CONTACTED,
            UNREPRESENTED_RESOURCE, UNREACHABLE),
        this::statesOfTheSubjectsResources);
    LifecyclePersistence.awaitValueAfterSettling(
        SETTLE,
        List.of(),
        () -> resourceChanges(events),
        "three Resources changed State and none of it was announced");
    assertEquals(
        List.of(),
        writtenBy(jdbcTemplate, RESOURCE_STATE_CHANGE),
        "so the handler that fires on every Resource state change does not fire for spawn");
  }
}
