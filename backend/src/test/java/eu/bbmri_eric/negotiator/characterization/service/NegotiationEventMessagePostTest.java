package eu.bbmri_eric.negotiator.characterization.service;

import static eu.bbmri_eric.negotiator.characterization.service.SeededNegotiationSubject.CREATOR;
import static eu.bbmri_eric.negotiator.characterization.service.SeededNegotiationSubject.NO_RESOURCES;
import static eu.bbmri_eric.negotiator.characterization.service.SeededNegotiationSubject.postCount;
import static eu.bbmri_eric.negotiator.characterization.service.SeededNegotiationSubject.posts;
import static eu.bbmri_eric.negotiator.characterization.service.SeededNegotiationSubject.putInState;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import eu.bbmri_eric.negotiator.characterization.adapter.LifecycleTestAdapter;
import eu.bbmri_eric.negotiator.characterization.adapter.LifecycleTestAdapterConfig;
import eu.bbmri_eric.negotiator.characterization.service.SeededNegotiationSubject.PostRow;
import eu.bbmri_eric.negotiator.util.IntegrationTest;
import eu.bbmri_eric.negotiator.util.WithMockNegotiatorUser;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;

/**
 * Pins the optional message a caller may send alongside a Negotiation Event: it becomes a post.
 *
 * <p>The Negotiation Lifecycle service takes an optional message on {@code sendEvent} and carries
 * it along with the Event. When one is supplied it turns into exactly one post on that Negotiation
 * - {@code PUBLIC}, carrying the message verbatim, attributed to the caller who sent the Event, and
 * timestamped when it was written rather than when the caller typed it.
 *
 * <p><b>This has nothing to do with the three Actions.</b> The message is handled on the path that
 * writes the new State, not by any Transition's Action, so the post appears on Transitions that
 * carry an effect and on Transitions that carry none alike. The rows below cover one of each,
 * deliberately.
 *
 * <p><b>The emptiness test is {@code isEmpty}, not {@code isBlank}.</b> A message of a single space
 * is therefore a post - pinned below, because it is the kind of edge a reimplementation tidies up
 * without noticing that it changed what a caller gets.
 *
 * <p>{@code negotiation-2} carries the traffic because the seed gives it no posts at all, so a post
 * observed after a send is one the send created, and no baseline count has to be believed.
 *
 * <p>{@code @DirtiesContext} per method restores the seed: this class moves a shared Negotiation
 * and leaves posts on it.
 */
@IntegrationTest(loadTestData = true)
@Import(LifecycleTestAdapterConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class NegotiationEventMessagePostTest {

  private static final String MESSAGE = "a reason given alongside the Event";

  /** Long enough for the asynchronous persist path to have written a post had it been going to. */
  private static final Duration SETTLE = Duration.ofSeconds(3);

  @Autowired LifecycleTestAdapter adapter;
  @Autowired JdbcTemplate jdbcTemplate;

  /**
   * One Transition that carries a post effect, one that carries none, and one fired from a State
   * only reachable by placing the subject there - enough to show the post is a property of sending
   * a message rather than of any particular Transition.
   */
  static Stream<Arguments> transitionsToSendAMessageWith() {
    return Stream.of(
        arguments("SUBMITTED", "APPROVE"),
        arguments("SUBMITTED", "DECLINE"),
        arguments("PAUSED", "ABANDON"));
  }

  @ParameterizedTest(name = "{1} from {0}")
  @MethodSource("transitionsToSendAMessageWith")
  @DisplayName("a message sent with an Event becomes exactly one PUBLIC post by the caller")
  @WithMockNegotiatorUser(id = SeededNegotiationSubject.ADMIN, authorities = "ROLE_ADMIN")
  void messageSentWithAnEvent_becomesOnePublicPostByTheCaller(String source, String event) {
    putInState(jdbcTemplate, NO_RESOURCES, source);
    assertEquals(0, postCount(jdbcTemplate, NO_RESOURCES), "the subject starts with no posts");
    LocalDateTime beforeSend = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);

    adapter.sendNegotiationEvent(NO_RESOURCES, event, MESSAGE);

    awaitState(NegotiationGraphV1.target(source, event));
    LifecyclePersistence.awaitValue(
        1, () -> postCount(jdbcTemplate, NO_RESOURCES), "the message becomes one post, not two");

    PostRow post = posts(jdbcTemplate, NO_RESOURCES).getFirst();
    assertEquals(MESSAGE, post.text(), "the post carries the message verbatim");
    assertEquals("PUBLIC", post.type(), "every message-borne post is PUBLIC, whatever the Event");
    assertEquals(
        SeededNegotiationSubject.ADMIN,
        post.createdBy(),
        "the post is attributed to the caller who sent the Event, not to the Negotiation's"
            + " creator, who is a different Person");
    assertThat(post.creationDate())
        .as("the timestamp is stamped when the post is written")
        .isBetween(beforeSend, LocalDateTime.now().plusMinutes(1));
  }

  /**
   * The other half of "attributed to the caller": the Negotiation's own creator sends an Event this
   * time, holding no admin role, and the post comes out in that caller's name. Between the two
   * tests the author tracks the caller rather than the Negotiation.
   */
  @Test
  @DisplayName("the post is by whoever sent the Event, admin or not")
  @WithMockNegotiatorUser(id = CREATOR)
  void thePostAuthor_followsTheCallerAndNotTheNegotiation() {
    putInState(jdbcTemplate, NO_RESOURCES, "IN_PROGRESS");

    adapter.sendNegotiationEvent(NO_RESOURCES, "ABANDON", MESSAGE);

    awaitState(NegotiationGraphV1.target("IN_PROGRESS", "ABANDON"));
    LifecyclePersistence.awaitValue(1, () -> postCount(jdbcTemplate, NO_RESOURCES));
    assertEquals(CREATOR, posts(jdbcTemplate, NO_RESOURCES).getFirst().createdBy());
  }

  @Test
  @DisplayName("an Event sent with no message at all creates no post")
  @WithMockNegotiatorUser(id = SeededNegotiationSubject.ADMIN, authorities = "ROLE_ADMIN")
  void eventWithoutAMessage_createsNoPost() {
    adapter.sendNegotiationEvent(NO_RESOURCES, "APPROVE");

    awaitState(NegotiationGraphV1.target("SUBMITTED", "APPROVE"));
    awaitNoPost();
  }

  @Test
  @DisplayName("an Event sent with an empty message creates no post")
  @WithMockNegotiatorUser(id = SeededNegotiationSubject.ADMIN, authorities = "ROLE_ADMIN")
  void eventWithAnEmptyMessage_createsNoPost() {
    adapter.sendNegotiationEvent(NO_RESOURCES, "APPROVE", "");

    awaitState(NegotiationGraphV1.target("SUBMITTED", "APPROVE"));
    awaitNoPost();
  }

  /**
   * The boundary of the previous test, and a sharp edge worth freezing: the check is only that the
   * message is not the empty string, so a message of whitespace is a post like any other.
   */
  @Test
  @DisplayName("an Event sent with a blank but non-empty message does create a post")
  @WithMockNegotiatorUser(id = SeededNegotiationSubject.ADMIN, authorities = "ROLE_ADMIN")
  void eventWithABlankMessage_createsAPostAllTheSame() {
    adapter.sendNegotiationEvent(NO_RESOURCES, "APPROVE", " ");

    awaitState(NegotiationGraphV1.target("SUBMITTED", "APPROVE"));
    LifecyclePersistence.awaitValue(1, () -> postCount(jdbcTemplate, NO_RESOURCES));
    assertEquals(" ", posts(jdbcTemplate, NO_RESOURCES).getFirst().text());
  }

  private void awaitNoPost() {
    LifecyclePersistence.awaitValueAfterSettling(
        SETTLE,
        0,
        () -> postCount(jdbcTemplate, NO_RESOURCES),
        "the Transition has landed and long enough has passed for a post to have been written");
  }

  private void awaitState(String expected) {
    LifecyclePersistence.awaitState(expected, () -> adapter.currentNegotiationState(NO_RESOURCES));
  }
}
