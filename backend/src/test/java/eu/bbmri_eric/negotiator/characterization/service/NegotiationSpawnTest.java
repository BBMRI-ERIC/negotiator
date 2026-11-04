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
 * Pins Spawn: what a Negotiation's arrival at {@code IN_PROGRESS} does to its Resources, and who is
 * told.
 *
 * <p>Three findings the State names do not suggest:
 *
 * <ul>
 *   <li>Spawn does not start a Resource in the graph's initial State. The initial State is {@code
 *       SUBMITTED}; Spawn writes {@code REPRESENTATIVE_CONTACTED}, or {@code
 *       REPRESENTATIVE_UNREACHABLE} when nobody represents the Resource. Starting Resources at the
 *       initial State would move every spawned Resource one step back and offer representatives an
 *       Event they are already past.
 *   <li>Spawn keys on the destination State, not the Event. Every arrival at {@code IN_PROGRESS}
 *       spawns, including the return from {@code PAUSED}, so tying it to {@code APPROVE} would lose
 *       that.
 *   <li>Spawn publishes no Resource state change. It writes States onto the link rows, so no
 *       listener of that event hears of them, the conclusion listener included. Every other writer
 *       of a Resource State publishes one, so routing Spawn through Transitions would start
 *       publishing events no consumer has seen before.
 * </ul>
 *
 * <p>The subject is {@link SeededNegotiationSubject#WITH_UNINITIALISED_RESOURCES}, the only seeded
 * Negotiation with Resources that have no State, plus the seed's only unrepresented Resource
 * attached by hand. Its two Resources have different representatives and a third researcher
 * created it, so each notification has one possible cause.
 *
 * <p>The tests read States from the link rows, notifications from the rows the handler writes, and
 * the absence of events through {@code @RecordApplicationEvents}. Every read polls through {@link
 * LifecyclePersistence}; every check that nothing happened waits a settle period first.
 *
 * <p>{@code @DirtiesContext} per method restores the seed: this class moves a shared Negotiation
 * and writes States onto shared link rows.
 */
@IntegrationTest(loadTestData = true)
@Import(LifecycleTestAdapterConfig.class)
@RecordApplicationEvents
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class NegotiationSpawnTest {

  /** Long enough for Spawn to have run, if it were going to. */
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
   * Spawn's starting States differ from the initial State, stated on its own.
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
   * Spawn is not a reset. A Resource that already has a State keeps it, and its representative is
   * not notified, because the code makes both decisions together.
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
   * Spawn keys on the State arrived in. The other Transition into {@code IN_PROGRESS} spawns too, so
   * Spawn cannot be tied to {@code APPROVE} alone.
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
   * No Transition ending anywhere else spawns, walked over the whole graph from {@link
   * NegotiationGraphV1}.
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
   * Spawn publishes no state change for the States it writes.
   *
   * <p>Three Resources change State and no Resource state change is published, so neither the
   * handler that fires on every such event nor the conclusion listener runs. That is why spawning
   * Resources into an unavailable State never concludes a Negotiation.
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
