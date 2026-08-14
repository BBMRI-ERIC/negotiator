package eu.bbmri_eric.negotiator.characterization.service;

import static eu.bbmri_eric.negotiator.characterization.service.SeededNegotiationSubject.NO_RESOURCES;
import static eu.bbmri_eric.negotiator.characterization.service.SeededNegotiationSubject.postFlags;
import static eu.bbmri_eric.negotiator.characterization.service.SeededNegotiationSubject.putInState;
import static eu.bbmri_eric.negotiator.characterization.service.SeededNegotiationSubject.setPostFlags;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import eu.bbmri_eric.negotiator.characterization.adapter.LifecycleTestAdapter;
import eu.bbmri_eric.negotiator.characterization.adapter.LifecycleTestAdapterConfig;
import eu.bbmri_eric.negotiator.characterization.service.NegotiationGraphV1.PostFlags;
import eu.bbmri_eric.negotiator.util.IntegrationTest;
import eu.bbmri_eric.negotiator.util.WithMockNegotiatorUser;
import java.time.Duration;
import java.util.List;
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
 * Pins what a Negotiation Transition does to the two flags that decide whether a Negotiation can be
 * posted to: public posts enabled, private posts enabled.
 *
 * <p>Three Transitions of the graph carry an effect and five carry none. ADR 0002 re-registers the
 * three beans that produce them, so what has to survive is the effect and not the bean: which
 * Transition leaves which flag where. Every Transition is fired for real and both flags are read
 * back off the row afterwards.
 *
 * <p><b>The expectation is computed, never typed out.</b> Each row applies {@link
 * NegotiationGraphV1#postEffects} to the flags the subject started with. That table is bound
 * Transition for Transition to the committed mechanical dump by {@link
 * NegotiationGraphV1BindingTest}, so an assertion here is anchored to an artifact nobody
 * transcribed, and it covers all eight Transitions rather than the ones someone remembered - which
 * is what makes the five *empty* entries assertions rather than omissions.
 *
 * <p><b>Every Transition is fired twice, from both flag settings.</b> An effect is only observable
 * when the flag it moves is not already where it puts it: from {@code (false, false)} an enabling
 * Transition shows, from {@code (true, true)} the disabling one does. Firing both arms means no row
 * can pass by starting where it was going to end up.
 *
 * <p><b>Why the subject is placed by hand.</b> Reaching a source State by driving a path would
 * apply that path's own post effects first, which is precisely what a statement about one
 * Transition must not have had happen to it. The State is therefore written onto the row, the way
 * {@link SeededResourceSubject} explains for the Resource graph.
 *
 * <p><b>Asynchrony.</b> Reads after a send poll through {@link LifecyclePersistence} under a
 * bounded timeout. The target State is always awaited first, and that is what makes a read of an
 * unchanged flag meaningful: the Actions of a Transition run while it executes, ahead of the State
 * being written, so a State that has landed is a Transition whose Actions have already run. Where
 * the whole point of the row is that nothing happened - the two abandon routes - the flags are
 * additionally settled on, so that "no effect" cannot be "not yet".
 *
 * <p>{@code @DirtiesContext} per method restores the seed: this class moves a shared Negotiation
 * and rewrites its flags.
 */
@IntegrationTest(loadTestData = true)
@Import(LifecycleTestAdapterConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class NegotiationPostEffectsTest {

  /** Long enough for the asynchronous persist path to have moved a flag had it been going to. */
  private static final Duration SETTLE = Duration.ofSeconds(3);

  private static final PostFlags NEITHER = new PostFlags(false, false);
  private static final PostFlags BOTH = new PostFlags(true, true);

  @Autowired LifecycleTestAdapter adapter;
  @Autowired JdbcTemplate jdbcTemplate;

  static Stream<Arguments> everyTransition() {
    return NegotiationGraphV1.TRANSITIONS.stream()
        .map(edge -> arguments(edge.source(), edge.event(), edge.target()));
  }

  @ParameterizedTest(name = "{0} --{1}--> {2}")
  @MethodSource("everyTransition")
  @DisplayName("every Transition leaves the post flags exactly where the pinned graph says")
  @WithMockNegotiatorUser(id = SeededNegotiationSubject.ADMIN, authorities = "ROLE_ADMIN")
  void transition_appliesItsPinnedPostEffects(String source, String event, String target) {
    for (PostFlags before : List.of(NEITHER, BOTH)) {
      putInState(jdbcTemplate, NO_RESOURCES, source);
      setPostFlags(jdbcTemplate, NO_RESOURCES, before);
      assertTrue(
          adapter.possibleNegotiationEvents(NO_RESOURCES).contains(event),
          "'%s' is not offered from '%s', so nothing about its effects can be observed"
              .formatted(event, source));

      adapter.sendNegotiationEvent(NO_RESOURCES, event);

      awaitState(target);
      LifecyclePersistence.awaitValue(
          before.after(NegotiationGraphV1.postEffects(source, event)),
          () -> postFlags(jdbcTemplate, NO_RESOURCES),
          "the flags a '%s' from '%s' leaves behind, starting from %s"
              .formatted(event, source, before));
    }
  }

  /**
   * The asymmetry this ticket exists to record, fired rather than read off the dump.
   *
   * <p>Both halves send the same Event, from the same Negotiation, with the same flags set, and
   * both end in {@code ABANDONED}. Abandoning a running Negotiation closes its posts; abandoning a
   * paused one leaves them open. The two routes out are therefore not interchangeable, and a
   * redesign that attaches one Action to "abandon" would change behaviour for every Negotiation
   * abandoned while paused.
   *
   * <p>The paused half settles before asserting, because "the flags did not move" is only a claim
   * about the system if enough time has passed for them to have moved.
   */
  @Test
  @DisplayName(
      "abandoning from PAUSED leaves the posts open; abandoning from IN_PROGRESS closes them")
  @WithMockNegotiatorUser(id = SeededNegotiationSubject.ADMIN, authorities = "ROLE_ADMIN")
  void theTwoAbandonRoutes_leaveThePostsInDifferentPlaces() {
    String target = NegotiationGraphV1.target("PAUSED", "ABANDON");

    putInState(jdbcTemplate, NO_RESOURCES, "PAUSED");
    setPostFlags(jdbcTemplate, NO_RESOURCES, BOTH);
    adapter.sendNegotiationEvent(NO_RESOURCES, "ABANDON");
    awaitState(target);
    LifecyclePersistence.awaitValueAfterSettling(
        SETTLE,
        BOTH,
        () -> postFlags(jdbcTemplate, NO_RESOURCES),
        "no Action is attached to this route, so both flags stay as they were");
    PostFlags afterAbandoningFromPaused = postFlags(jdbcTemplate, NO_RESOURCES);

    putInState(jdbcTemplate, NO_RESOURCES, "IN_PROGRESS");
    setPostFlags(jdbcTemplate, NO_RESOURCES, BOTH);
    adapter.sendNegotiationEvent(NO_RESOURCES, "ABANDON");
    awaitState(target);
    LifecyclePersistence.awaitValue(
        NEITHER,
        () -> postFlags(jdbcTemplate, NO_RESOURCES),
        "abandoning a running Negotiation closes both kinds of post");

    assertNotEquals(
        afterAbandoningFromPaused,
        postFlags(jdbcTemplate, NO_RESOURCES),
        "the same Event into the same State left the two Negotiations in different places, which is"
            + " the whole of the asymmetry");
    assertEquals(
        NegotiationGraphV1.target("IN_PROGRESS", "ABANDON"),
        target,
        "the contrast is only meaningful because both routes end in the same State");
  }

  private void awaitState(String expected) {
    LifecyclePersistence.awaitState(expected, () -> adapter.currentNegotiationState(NO_RESOURCES));
  }
}
