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
 * <p>Of the eight strategies these are the five whose behaviour turns on a State or on a lifecycle
 * event, and they are the reason the parent PRD calls this seam the likeliest silent breakage of
 * the enum removal: each keys on an enum constant, outside any registry, and none of them is
 * covered by a test that fires a real Transition. The other three do not key on lifecycle identity
 * and are out of scope, deliberately - the parent ticket's "seven handlers" is a miscount and five
 * is the number that matters.
 *
 * <p><b>Every handler is observed through the row it writes, and through the recorded application
 * event that triggered it - never through mail.</b> A notification row is a handler's only durable
 * effect; sending mail is a separate listener on top of it, and no test has a mail server. So each
 * arm below asserts the lifecycle event was published and then asserts exactly which handlers wrote
 * something and to whom. {@link HandlerNotifications} explains why a title identifies a handler
 * here.
 *
 * <p><b>What the walk establishes, beyond the obvious.</b>
 *
 * <ul>
 *   <li>Three of the five key on the <em>destination State</em> and not on the Event, so every
 *       arrival at a State behaves alike however it was reached.
 *   <li>The Resource state change handler does <em>not</em> key on any State: it fires on every
 *       published Resource state change, from the Lifecycle and from the override path alike. It is
 *       lifecycle-keyed only in its content, which is built from the two States' labels.
 *   <li>The pending reminder handler ignores the Negotiation's own State entirely - an abandoned
 *       Negotiation whose Resource is still marked as contacted goes on reminding its
 *       representatives.
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
   * <p>Carried on the Negotiation with no Resources, so that what is counted is only what these two
   * handlers wrote: the in-progress handler still runs on arrival at {@code IN_PROGRESS}, but with
   * no Resource to initialise it has nobody to tell, which is itself worth seeing.
   *
   * <p>Both handlers key on the State arrived in and neither on the Event, so the expectation is
   * indexed by destination rather than by Transition - and the two Transitions that end in {@code
   * ABANDONED} produce identical notifications even though they differ in every other side effect.
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
   * <p>Its firing condition is the destination State alone: both Transitions that end at {@code
   * IN_PROGRESS} fire it and no other Transition does. The Resource States are cleared between arms
   * because the handler only acts on Resources without one - see {@link NegotiationSpawnTest}.
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
   * The Resource state change handler, which is the odd one of the five: it has no firing condition
   * at all beyond the event existing.
   *
   * <p>Both producers of that event reach it - a Transition of the Lifecycle and the governance
   * override that consults no Definition graph - and it notifies the Negotiation's creator either
   * way. Only the message differs, and it differs by carrying the two States' user-facing labels,
   * which is where this handler's dependence on lifecycle identity actually sits. The labels are
   * read from the committed metadata artifact rather than transcribed, so a label edit fails in one
   * place.
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
   * The pending reminder handler, the one of the five keyed on a Resource State rather than on a
   * Negotiation State: it reminds the representatives of Resources still sitting at {@code
   * REPRESENTATIVE_CONTACTED}, and nobody else.
   *
   * <p>Its own lookup is by creation date - Negotiations created exactly five days ago - so both
   * halves of its condition are walked: the State, and the age.
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
   * The reminder handler looks at the Resource and never at the Negotiation, so a Negotiation that
   * has been abandoned goes on reminding its representatives to attend to it. Recorded as
   * behaviour; the aggregation ADR 0007 introduces is the natural place to decide whether it should
   * stay.
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
   * The condition that sits underneath all five: the single dispatcher between the events and the
   * handlers is a transactional listener, so an event published outside a transaction reaches no
   * handler at all.
   *
   * <p>Every lifecycle event in the system is published from inside one, so this is invisible
   * there. The reminder is the exception: its scheduled publisher is not transactional, which makes
   * this the difference between a scheduled reminder that arrives and one that does not. Pinned as
   * it stands - the same event, published the same way, twice, differing only in the transaction.
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
   * The five handlers are exactly the ones this class walks, and the enumeration is a statement in
   * its own right: the parent ticket says seven, the accurate count is five, and a sixth appearing
   * in {@link LifecycleHandler} without a walk of its own would go unnoticed otherwise.
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
