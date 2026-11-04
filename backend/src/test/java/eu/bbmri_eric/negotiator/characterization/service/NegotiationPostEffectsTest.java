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
 * <p>Three Transitions have an effect and five have none. The beans that produce the effects may
 * change, so the tests pin the effect: which Transition leaves which flag where. Every Transition
 * is fired and both flags are read back from the row.
 *
 * <p>Each row applies {@link NegotiationGraphV1#postEffects} to the starting flags. {@link
 * NegotiationGraphV1BindingTest} binds that table to the committed dump, so the five empty entries
 * are real assertions too.
 *
 * <p>Every Transition fires twice, once from {@code (false, false)} and once from {@code (true,
 * true)}, because an effect only shows when its flag is not already where it puts it.
 *
 * <p>The source State is written onto the row, because reaching it by firing a path would apply
 * that path's post effects first.
 *
 * <p>Reads poll through {@link LifecyclePersistence}, waiting for the target State first. A
 * Transition's Actions run before the State is written, so once the State lands the Actions have
 * run. For the two abandon routes, where nothing should happen, the flags also get a settle period.
 *
 * <p>{@code @DirtiesContext} per method restores the seed: this class moves a shared Negotiation
 * and rewrites its flags.
 */
@IntegrationTest(loadTestData = true)
@Import(LifecycleTestAdapterConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class NegotiationPostEffectsTest {

  /** Long enough for the asynchronous persist path to have moved a flag, if it were going to. */
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
   * The two abandon routes, fired rather than read from the dump.
   *
   * <p>Both send the same Event from the same flags and end in {@code ABANDONED}. Abandoning a
   * running Negotiation closes its posts; abandoning a paused one leaves them open. Attaching one
   * Action to {@code ABANDON} would change behaviour for every Negotiation abandoned while paused.
   *
   * <p>The paused half waits a settle period first, so "the flags did not move" is not "not yet".
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
