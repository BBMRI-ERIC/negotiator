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
 * <p>A message passed to {@code sendEvent} becomes exactly one post on the Negotiation: {@code
 * PUBLIC}, with the message verbatim, by the caller who sent the Event, and timestamped when it was
 * written.
 *
 * <p>The code that writes the new State creates the post, not any Transition's Action, so it
 * appears whether or not the Transition has an effect. The rows below cover both.
 *
 * <p>The check is {@code isEmpty}, not {@code isBlank}, so a single space becomes a post. It is
 * pinned because a reimplementation could easily change it.
 *
 * <p>The tests use {@code negotiation-2} because the seed gives it no posts, so any post seen after
 * a send came from that send.
 *
 * <p>{@code @DirtiesContext} per method restores the seed: this class moves a shared Negotiation
 * and leaves posts on it.
 */
@IntegrationTest(loadTestData = true)
@Import(LifecycleTestAdapterConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class NegotiationEventMessagePostTest {

  private static final String MESSAGE = "a reason given alongside the Event";

  /** Long enough for the asynchronous persist path to have written a post, if it were going to. */
  private static final Duration SETTLE = Duration.ofSeconds(3);

  @Autowired LifecycleTestAdapter adapter;
  @Autowired JdbcTemplate jdbcTemplate;

  /**
   * One Transition with a post effect, one without, and one fired from a State the subject is
   * placed in, to show the post comes from the message and not the Transition.
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
   * The creator, with no admin role, sends the Event and the post is in the creator's name. With the
   * previous test this shows the author follows the caller.
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
   * The check only rejects the empty string, so a message of whitespace becomes a post.
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
