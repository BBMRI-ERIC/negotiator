package eu.bbmri_eric.negotiator.characterization.service;

import static eu.bbmri_eric.negotiator.characterization.service.HandlerNotifications.LifecycleHandler.NEGOTIATION_IN_PROGRESS;
import static eu.bbmri_eric.negotiator.characterization.service.HandlerNotifications.LifecycleHandler.PENDING_NEGOTIATION_REMINDER;
import static eu.bbmri_eric.negotiator.characterization.service.HandlerNotifications.LifecycleHandler.RESOURCE_STATE_CHANGE;
import static eu.bbmri_eric.negotiator.characterization.service.HandlerNotifications.forNegotiation;
import static eu.bbmri_eric.negotiator.characterization.service.HandlerNotifications.handlersThatFired;
import static eu.bbmri_eric.negotiator.characterization.service.HandlerNotifications.recipientsOf;
import static eu.bbmri_eric.negotiator.characterization.service.SeededNegotiationSubject.NO_RESOURCES;
import static eu.bbmri_eric.negotiator.characterization.service.SeededNegotiationSubject.REPRESENTATIVE_OF_FIRST_RESOURCE;
import static eu.bbmri_eric.negotiator.characterization.service.SeededNegotiationSubject.REPRESENTATIVE_OF_SECOND_RESOURCE;
import static eu.bbmri_eric.negotiator.characterization.service.SeededNegotiationSubject.WITH_UNINITIALISED_RESOURCES;
import static eu.bbmri_eric.negotiator.characterization.service.SeededNegotiationSubject.clearResourceStates;
import static eu.bbmri_eric.negotiator.characterization.service.SeededNegotiationSubject.putInState;
import static eu.bbmri_eric.negotiator.characterization.service.SeededResourceSubject.ADMIN;
import static eu.bbmri_eric.negotiator.characterization.service.SeededResourceSubject.CREATOR;
import static eu.bbmri_eric.negotiator.characterization.service.SeededResourceSubject.NEGOTIATION;
import static eu.bbmri_eric.negotiator.characterization.service.SeededResourceSubject.REPRESENTATIVE;
import static eu.bbmri_eric.negotiator.characterization.service.SeededResourceSubject.RESOURCE;
import static eu.bbmri_eric.negotiator.characterization.service.SeededResourceSubject.RESOURCE_ROW_ID;
import static eu.bbmri_eric.negotiator.characterization.service.SeededResourceSubject.SECOND_REPRESENTATIVE;
import static eu.bbmri_eric.negotiator.characterization.service.SeededResourceSubject.authenticateAs;
import static eu.bbmri_eric.negotiator.characterization.service.SeededResourceSubject.putNegotiationInState;
import static eu.bbmri_eric.negotiator.characterization.service.SeededResourceSubject.putResourceInState;
import static eu.bbmri_eric.negotiator.characterization.service.StateChangeEvents.negotiationChanges;
import static eu.bbmri_eric.negotiator.characterization.service.StateChangeEvents.resourceChanges;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.bbmri_eric.negotiator.characterization.adapter.LifecycleTestAdapter;
import eu.bbmri_eric.negotiator.characterization.adapter.LifecycleTestAdapterConfig;
import eu.bbmri_eric.negotiator.characterization.rest.CanonicalJson;
import eu.bbmri_eric.negotiator.characterization.service.HandlerNotifications.LifecycleHandler;
import eu.bbmri_eric.negotiator.characterization.service.HandlerNotifications.NotificationRow;
import eu.bbmri_eric.negotiator.notification.internal.PendingNegotiationReminderEvent;
import eu.bbmri_eric.negotiator.util.IntegrationTest;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Pins the firing condition of the five notification strategies that key on lifecycle identity: the
 * in-progress handler, the submission handler, the status change handler, the Resource state change
 * handler and the pending reminder handler.
 *
 * <p>Of the eight strategies, only these five depend on a State or a lifecycle event. Each keys on
 * an enum constant outside any registry, so removing the enums could break them without a compile
 * error.
 *
 * <p>Each arm checks that the lifecycle event was published, then exactly which handlers wrote a
 * notification row and to whom. No test reads mail; {@link HandlerNotifications} explains why, and
 * why a title identifies a handler here.
 *
 * <p>What the walk shows:
 *
 * <ul>
 *   <li>Three of the five key on the <em>destination State</em> and not on the Event, so every
 *       arrival at a State behaves alike however it was reached.
 *   <li>The Resource state change handler keys on no State: it fires on every published Resource
 *       state change, from a Transition or an Override. Only its message uses the two States'
 *       labels.
 *   <li>The pending reminder handler ignores the Negotiation's State, so an abandoned Negotiation
 *       whose Resource is still contacted keeps reminding its representatives.
 *   <li>None of the five is reached at all unless the triggering event is published inside a
 *       transaction, because the one dispatcher between the events and the handlers is a
 *       transactional listener. The scheduled reminder is published outside one.
 * </ul>
 *
 * <p>{@code @DirtiesContext} per method restores the seed: this class drives shared Negotiations
 * and empties the notification table between arms.
 */
@IntegrationTest(loadTestData = true)
@Import(LifecycleTestAdapterConfig.class)
@RecordApplicationEvents
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class LifecycleNotificationHandlerTest {

  /** Long enough for an asynchronously dispatched handler to have written a row. */
  private static final Duration SETTLE = Duration.ofSeconds(3);

  /** The committed metadata the Resource state change handler's message is built out of. */
  private static final Map<String, String> RESOURCE_STATE_LABELS =
      CanonicalJson.publishedLabels("characterization/rest/resource-states.json", "states");

  /** One notification as its recipient meets it: a title, and who it went to. */
  private record Notice(String title, long recipient) {}

  @Autowired LifecycleTestAdapter adapter;
  @Autowired JdbcTemplate jdbcTemplate;
  @Autowired ApplicationEvents events;
  @Autowired ApplicationEventPublisher publisher;
  @Autowired PlatformTransactionManager transactionManager;

  @AfterEach
  void clearAuthentication() {
    SecurityContextHolder.clearContext();
  }

  private Set<Notice> noticesFor(String negotiationId) {
    return forNegotiation(jdbcTemplate, negotiationId).stream()
        .map(row -> new Notice(row.title(), row.recipientId()))
        .collect(Collectors.toSet());
  }

  /**
   * The administrators, as the submission handler itself asks for them: the {@code admin} column.
   */
  private Set<Long> administrators() {
    return new HashSet<>(
        jdbcTemplate.queryForList("select id from person where admin is true", Long.class));
  }

  private Set<Notice> expectedFor(String destination) {
    return switch (destination) {
      case "SUBMITTED" -> {
        Set<Notice> notices = new HashSet<>();
        notices.add(new Notice("Negotiation Submission Confirmed", CREATOR));
        administrators().forEach(admin -> notices.add(new Notice("New Request", admin)));
        yield notices;
      }
      case "IN_PROGRESS", "DECLINED", "ABANDONED" ->
          Set.of(new Notice("Request Status Update", CREATOR));
      default -> Set.of();
    };
  }

  /**
   * The submission handler and the status change handler, walked over the whole Negotiation graph.
   *
   * <p>Uses the Negotiation with no Resources, so only these two handlers write anything: the
   * in-progress handler still runs on arrival at {@code IN_PROGRESS} but has nobody to tell.
   *
   * <p>Both handlers key on the State arrived in, not the Event, so the expectation is indexed by
   * destination. The two Transitions into {@code ABANDONED} produce identical notifications though
   * their other effects differ.
   */
  @Test
  @DisplayName(
      "the submission and status change handlers fire on the State arrived in, over the whole graph")
  void negotiationHandlers_fireOnTheirDestinationStateAndNowhereElse() {
    authenticateAs(ADMIN);
    for (NegotiationGraphV1.Edge edge : NegotiationGraphV1.TRANSITIONS) {
      HandlerNotifications.clear(jdbcTemplate);
      putInState(jdbcTemplate, NO_RESOURCES, edge.source());
      assertTrue(
          adapter.possibleNegotiationEvents(NO_RESOURCES).contains(edge.event()),
          "'%s' is not offered from '%s'".formatted(edge.event(), edge.source()));

      adapter.sendNegotiationEvent(NO_RESOURCES, edge.event());

      LifecyclePersistence.awaitState(
          edge.target(), () -> adapter.currentNegotiationState(NO_RESOURCES));
      assertThat(negotiationChanges(events))
          .as("the handlers' trigger is a published state change")
          .last()
          .isEqualTo(
              new StateChangeEvents.NegotiationStateChange(
                  NO_RESOURCES, edge.source(), edge.target(), edge.event()));
      LifecyclePersistence.awaitValueAfterSettling(
          SETTLE,
          expectedFor(edge.target()),
          () -> noticesFor(NO_RESOURCES),
          "what arriving at '%s' by '%s' notifies".formatted(edge.target(), edge.event()));
    }
  }

  /**
   * The in-progress handler, walked over the same graph on a Negotiation that does have Resources.
   *
   * <p>It keys on the destination State alone: both Transitions into {@code IN_PROGRESS} fire it and
   * no other does. The test clears the Resource States between arms because the handler only acts
   * on Resources without one; see {@link NegotiationSpawnTest}.
   */
  @Test
  @DisplayName("the in-progress handler fires on every arrival at IN_PROGRESS and on nothing else")
  void inProgressHandler_firesOnItsDestinationStateAndNowhereElse() {
    authenticateAs(ADMIN);
    for (NegotiationGraphV1.Edge edge : NegotiationGraphV1.TRANSITIONS) {
      HandlerNotifications.clear(jdbcTemplate);
      clearResourceStates(jdbcTemplate, WITH_UNINITIALISED_RESOURCES);
      putInState(jdbcTemplate, WITH_UNINITIALISED_RESOURCES, edge.source());
      assertTrue(
          adapter.possibleNegotiationEvents(WITH_UNINITIALISED_RESOURCES).contains(edge.event()),
          "'%s' is not offered from '%s'".formatted(edge.event(), edge.source()));

      adapter.sendNegotiationEvent(WITH_UNINITIALISED_RESOURCES, edge.event());

      LifecyclePersistence.awaitState(
          edge.target(), () -> adapter.currentNegotiationState(WITH_UNINITIALISED_RESOURCES));
      Set<Long> expected =
          edge.target().equals("IN_PROGRESS")
              ? Set.of(REPRESENTATIVE_OF_FIRST_RESOURCE, REPRESENTATIVE_OF_SECOND_RESOURCE)
              : Set.of();
      LifecyclePersistence.awaitValueAfterSettling(
          SETTLE,
          expected,
          () -> recipientsOf(jdbcTemplate, NEGOTIATION_IN_PROGRESS, WITH_UNINITIALISED_RESOURCES),
          "who arriving at '%s' by '%s' told about their Resources"
              .formatted(edge.target(), edge.event()));
    }
  }

  /**
   * The Resource state change handler, which fires on every such event with no further condition.
   *
   * <p>Both producers reach it, a Transition and an Override, and it notifies the Negotiation's
   * creator either way. Only the message differs, through the two States' labels. The test reads the
   * labels from the committed metadata, so a label edit fails in one place.
   */
  @Test
  @DisplayName(
      "the Resource state change handler fires on every published Resource state change, from both producers")
  void resourceStateChangeHandler_firesOnEveryStateChangeFromEitherProducer() {
    putNegotiationInState(jdbcTemplate, ResourceGraphV1.REQUIRED_PARENT_STATE);
    putResourceInState(jdbcTemplate, "REPRESENTATIVE_CONTACTED");
    authenticateAs(REPRESENTATIVE);

    adapter.sendResourceEvent(NEGOTIATION, RESOURCE, "MARK_AS_CHECKING_AVAILABILITY");

    LifecyclePersistence.awaitState(
        "CHECKING_AVAILABILITY", () -> adapter.currentResourceState(NEGOTIATION, RESOURCE));
    LifecyclePersistence.awaitValue(
        List.of(messageFor("REPRESENTATIVE_CONTACTED", "CHECKING_AVAILABILITY")),
        () -> messagesWrittenByTheResourceHandler(),
        "a Transition notifies the Negotiation's creator");
    assertEquals(Set.of(CREATOR), recipientsOf(jdbcTemplate, RESOURCE_STATE_CHANGE, NEGOTIATION));

    HandlerNotifications.clear(jdbcTemplate);
    authenticateAs(ADMIN);

    adapter.overrideResourceStates(NEGOTIATION, List.of(RESOURCE_ROW_ID), "RESOURCE_AVAILABLE");

    LifecyclePersistence.awaitState(
        "RESOURCE_AVAILABLE", () -> adapter.currentResourceState(NEGOTIATION, RESOURCE));
    LifecyclePersistence.awaitValue(
        List.of(messageFor("CHECKING_AVAILABILITY", "RESOURCE_AVAILABLE")),
        () -> messagesWrittenByTheResourceHandler(),
        "and so does an override, which traces no Transition at all");
    assertEquals(Set.of(CREATOR), recipientsOf(jdbcTemplate, RESOURCE_STATE_CHANGE, NEGOTIATION));
    assertThat(resourceChanges(events))
        .as("both arms went through the one event both producers publish")
        .hasSize(2);
  }

  private List<String> messagesWrittenByTheResourceHandler() {
    return HandlerNotifications.writtenBy(jdbcTemplate, RESOURCE_STATE_CHANGE).stream()
        .map(NotificationRow::message)
        .toList();
  }

  private String messageFor(String fromState, String toState) {
    String title =
        jdbcTemplate.queryForObject(
            "select json_extract_path_text(payload::json, 'project', 'title') from negotiation"
                + " where id = ?",
            String.class,
            NEGOTIATION);
    return "Resource %s had a change of status in your request %s, from %s to %s"
        .formatted(
            RESOURCE,
            title,
            RESOURCE_STATE_LABELS.get(fromState),
            RESOURCE_STATE_LABELS.get(toState));
  }

  /**
   * The pending reminder handler, the one keyed on a Resource State: it reminds the representatives
   * of Resources still at {@code REPRESENTATIVE_CONTACTED}, and nobody else.
   *
   * <p>It looks up Negotiations created exactly five days ago, so the test walks both the State and
   * the age.
   */
  @Test
  @DisplayName(
      "the pending reminder handler reminds the representatives of contacted Resources only")
  void pendingReminderHandler_firesForContactedResourcesOnly() {
    SeededNegotiationSubject.setCreationDateDaysAgo(jdbcTemplate, NEGOTIATION, 5);
    putResourceInState(jdbcTemplate, "REPRESENTATIVE_CONTACTED");

    fireTheReminder();

    LifecyclePersistence.awaitValue(
        Set.of(REPRESENTATIVE, SECOND_REPRESENTATIVE),
        () -> recipientsOf(jdbcTemplate, PENDING_NEGOTIATION_REMINDER, NEGOTIATION),
        "every representative of the contacted Resource, and only those");

    HandlerNotifications.clear(jdbcTemplate);
    putResourceInState(jdbcTemplate, "CHECKING_AVAILABILITY");

    fireTheReminder();

    LifecyclePersistence.awaitValueAfterSettling(
        SETTLE,
        Set.of(),
        () -> handlersThatFired(jdbcTemplate),
        "a Resource that has moved past being contacted reminds nobody");
  }

  @Test
  @DisplayName("the pending reminder handler is silent about a Negotiation of any other age")
  void pendingReminderHandler_isSilentAboutANegotiationOfAnotherAge() {
    putResourceInState(jdbcTemplate, "REPRESENTATIVE_CONTACTED");
    SeededNegotiationSubject.setCreationDateDaysAgo(jdbcTemplate, NEGOTIATION, 0);

    fireTheReminder();

    LifecyclePersistence.awaitValueAfterSettling(
        SETTLE, Set.of(), () -> handlersThatFired(jdbcTemplate));
  }

  /**
   * The reminder handler looks at the Resource and never at the Negotiation, so an abandoned
   * Negotiation keeps reminding its representatives. Pinned as it is, though it looks unintended.
   */
  @Test
  @DisplayName("the pending reminder handler ignores the Negotiation's own State")
  void pendingReminderHandler_remindsEvenAboutAnAbandonedNegotiation() {
    SeededNegotiationSubject.setCreationDateDaysAgo(jdbcTemplate, NEGOTIATION, 5);
    putResourceInState(jdbcTemplate, "REPRESENTATIVE_CONTACTED");
    putNegotiationInState(jdbcTemplate, "ABANDONED");

    fireTheReminder();

    LifecyclePersistence.awaitValue(
        Set.of(REPRESENTATIVE, SECOND_REPRESENTATIVE),
        () -> recipientsOf(jdbcTemplate, PENDING_NEGOTIATION_REMINDER, NEGOTIATION));
  }

  /**
   * The dispatcher between the events and all five handlers is a transactional listener, so an
   * event published outside a transaction reaches no handler.
   *
   * <p>Every lifecycle event is published inside a transaction, but the reminder's scheduled
   * publisher is not transactional, so this decides whether a scheduled reminder arrives. The test
   * publishes the same event twice, once with and once without a transaction.
   */
  @Test
  @DisplayName("an event published outside a transaction reaches no handler")
  void noHandlerIsReached_whenTheEventIsPublishedOutsideATransaction() {
    SeededNegotiationSubject.setCreationDateDaysAgo(jdbcTemplate, NEGOTIATION, 5);
    putResourceInState(jdbcTemplate, "REPRESENTATIVE_CONTACTED");

    publisher.publishEvent(new PendingNegotiationReminderEvent(this));

    LifecyclePersistence.awaitValueAfterSettling(
        SETTLE,
        Set.of(),
        () -> handlersThatFired(jdbcTemplate),
        "published outside a transaction, the event is dropped");

    fireTheReminder();

    LifecyclePersistence.awaitValue(
        Set.of(PENDING_NEGOTIATION_REMINDER),
        () -> handlersThatFired(jdbcTemplate),
        "and published inside one, the very same event is delivered");
  }

  /** Publishes the reminder event inside a transaction, which is what the dispatcher waits for. */
  private void fireTheReminder() {
    new TransactionTemplate(transactionManager)
        .executeWithoutResult(
            status -> publisher.publishEvent(new PendingNegotiationReminderEvent(this)));
  }

  /**
   * Fails if a sixth handler appears in {@link LifecycleHandler} without a walk of its own.
   */
  @Test
  @DisplayName("the five lifecycle-keyed handlers are the five this class pins")
  void theFiveLifecycleKeyedHandlers_areAllWalked() {
    assertEquals(
        Set.of(
            LifecycleHandler.NEGOTIATION_IN_PROGRESS,
            LifecycleHandler.NEGOTIATION_SUBMISSION,
            LifecycleHandler.NEGOTIATION_STATUS_CHANGE,
            LifecycleHandler.RESOURCE_STATE_CHANGE,
            LifecycleHandler.PENDING_NEGOTIATION_REMINDER),
        Set.of(LifecycleHandler.values()));
  }
}
