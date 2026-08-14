package eu.bbmri_eric.negotiator.characterization.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import eu.bbmri_eric.negotiator.characterization.adapter.LifecycleTestAdapter;
import eu.bbmri_eric.negotiator.characterization.adapter.LifecycleTestAdapterConfig;
import eu.bbmri_eric.negotiator.util.IntegrationTest;
import eu.bbmri_eric.negotiator.util.WithMockNegotiatorUser;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.annotation.DirtiesContext;

/**
 * Pins the shape of the Negotiation Definition graph: from which State, which Event leads to which
 * State.
 *
 * <p>Every one of the eight Transitions is fired for real against the Lifecycle service and the
 * resulting State is read back, so a Transition that is missing, misdirected or newly invented
 * fails here rather than in production. The rows come from {@link NegotiationGraphV1}, which is a
 * transcription of the mechanically produced dump; this class is what makes that transcription
 * worth trusting.
 *
 * <p>The caller that drives is always an admin, because an admin is the one caller offered every
 * Transition - the authority rules that narrow this for everyone else are pinned by {@link
 * NegotiationAuthorityParityTest} in the four States seeded data occupies, and here in the three it
 * does not. Sending an Event drives an asynchronous persist path, so every resulting State is read
 * back under a bounded wait; no assertion is made immediately after a send, and no method here is
 * transactional, since Awaitility polls on a thread that would not see the test's transaction.
 *
 * <p>{@code negotiation-2} carries the traffic: it is seeded {@code SUBMITTED} and has no Resources
 * at all, so driving it does not Spawn Resource Lifecycles or the notifications that follow them.
 * Only the {@code DRAFT} row needs another subject, since {@code negotiation-6} is the one
 * Negotiation seeded in that State.
 */
@IntegrationTest(loadTestData = true)
@Import(LifecycleTestAdapterConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class NegotiationTransitionParityTest {

  private static final String NO_RESOURCES = "negotiation-2";
  private static final String SEEDED_IN_DRAFT = "negotiation-6";

  /** Person 108, who created both Negotiations above and holds no admin role. */
  private static final String CREATOR = "TheResearcher";

  @Autowired LifecycleTestAdapter adapter;

  @Autowired UserDetailsService userDetailsService;

  /**
   * Each row is one Transition of the graph, together with the Events needed to bring the subject
   * Negotiation to that Transition's source State first.
   */
  static Stream<Arguments> everyTransition() {
    return Stream.of(
        arguments(SEEDED_IN_DRAFT, List.of(), "DRAFT", "SUBMIT", "SUBMITTED"),
        arguments(NO_RESOURCES, List.of(), "SUBMITTED", "APPROVE", "IN_PROGRESS"),
        arguments(NO_RESOURCES, List.of(), "SUBMITTED", "DECLINE", "DECLINED"),
        arguments(NO_RESOURCES, List.of("APPROVE"), "IN_PROGRESS", "PAUSE", "PAUSED"),
        arguments(NO_RESOURCES, List.of("APPROVE"), "IN_PROGRESS", "ABANDON", "ABANDONED"),
        arguments(NO_RESOURCES, List.of("APPROVE"), "IN_PROGRESS", "CONCLUDE", "CONCLUDED"),
        arguments(NO_RESOURCES, List.of("APPROVE", "PAUSE"), "PAUSED", "UNPAUSE", "IN_PROGRESS"),
        arguments(NO_RESOURCES, List.of("APPROVE", "PAUSE"), "PAUSED", "ABANDON", "ABANDONED"));
  }

  @ParameterizedTest(name = "{2} --{3}--> {4}")
  @MethodSource("everyTransition")
  @DisplayName("every Transition of the Negotiation graph leads where the graph says")
  @WithMockNegotiatorUser(id = 101L, authorities = "ROLE_ADMIN")
  void transition_leadsToItsTarget(
      String negotiationId, List<String> pathToSource, String source, String event, String target) {
    pathToSource.forEach(step -> drive(negotiationId, step));

    assertEquals(
        source,
        adapter.currentNegotiationState(negotiationId),
        "the path taken did not end in the State this Transition starts from");
    assertTrue(
        adapter.possibleNegotiationEvents(negotiationId).contains(event),
        "'%s' is not offered from '%s', so the Transition cannot even be attempted"
            .formatted(event, source));

    adapter.sendNegotiationEvent(negotiationId, event);

    awaitState(negotiationId, target);
  }

  /**
   * The three States no seeded Negotiation occupies. Their Possible Events are pinned here rather
   * than in the authority suite because reaching them means driving a Negotiation there first, and
   * an empty offering is exactly how a terminal State is observable from outside.
   *
   * <p>The same rows serve both callers, because the pairing is itself the point: none of the three
   * States is left by an admin-secured Transition, so in all three the creator is offered exactly
   * what the admin is. That is the opposite of {@code SUBMITTED}, where the whole offering is
   * admin-only.
   *
   * <p>What each caller is expected to be offered is computed from {@link NegotiationGraphV1}
   * rather than typed out, which is this suite's convention throughout: the table is bound edge for
   * edge to the committed mechanical dump by {@link NegotiationGraphV1BindingTest}, so the
   * expectation is anchored to an artifact nobody transcribed, and it stays complete if the graph
   * turns out to be larger than the rows someone remembered to type.
   */
  static Stream<Arguments> statesReachedOnlyByDriving() {
    return Stream.of(
        arguments(List.of("APPROVE", "PAUSE"), "PAUSED"),
        arguments(List.of("DECLINE"), "DECLINED"),
        arguments(List.of("APPROVE", "CONCLUDE"), "CONCLUDED"));
  }

  @ParameterizedTest(name = "an admin at {1}")
  @MethodSource("statesReachedOnlyByDriving")
  @DisplayName("Possible Events as an admin, in the States no seeded Negotiation occupies")
  @WithMockNegotiatorUser(id = 101L, authorities = "ROLE_ADMIN")
  void possibleEvents_inDrivenState_matchTheGraph(List<String> pathToState, String state) {
    pathToState.forEach(step -> drive(NO_RESOURCES, step));

    assertEquals(state, adapter.currentNegotiationState(NO_RESOURCES));
    assertEquals(
        NegotiationGraphV1.possibleEventsForAdmin(state),
        adapter.possibleNegotiationEvents(NO_RESOURCES),
        "the offering must be exactly the Transitions the pinned graph leaves '%s' by"
            .formatted(state));
  }

  /**
   * The same three States seen by the Negotiation's own creator, which the seeded corpus cannot
   * reach: {@code PAUSED}, {@code DECLINED} and {@code CONCLUDED} all lie behind an admin-secured
   * Transition, so only an admin can put a Negotiation into them.
   *
   * <p>The driving is therefore done as an admin and only the reading is done as the creator - the
   * caller is swapped in the middle of the method, because a single {@code @WithUserDetails} would
   * fix one caller for the whole of it. The swap builds the creator's {@code Authentication} the
   * way {@code @WithUserDetails} does, from the same {@code UserDetailsService} the annotation
   * resolves, so the caller the service sees is the same either way.
   */
  @ParameterizedTest(name = "the creator at {1}")
  @MethodSource("statesReachedOnlyByDriving")
  @DisplayName("Possible Events as the creator, in the States no seeded Negotiation occupies")
  @WithMockNegotiatorUser(id = 101L, authorities = "ROLE_ADMIN")
  void possibleEvents_inDrivenState_asCreator_matchTheGraph(
      List<String> pathToState, String state) {
    pathToState.forEach(step -> drive(NO_RESOURCES, step));
    assertEquals(state, adapter.currentNegotiationState(NO_RESOURCES));

    asCreator(
        () ->
            assertEquals(
                NegotiationGraphV1.possibleEventsForCreator(state),
                adapter.possibleNegotiationEvents(NO_RESOURCES),
                "the creator is offered the Transitions leaving '%s' less the admin-secured ones"
                    .formatted(state)));
  }

  /**
   * Runs {@code assertions} as the creator of {@link #NO_RESOURCES}, restoring the original caller
   * afterwards so the rest of the method - and the {@code @DirtiesContext} teardown - is
   * unaffected.
   */
  private void asCreator(Runnable assertions) {
    SecurityContext original = SecurityContextHolder.getContext();
    try {
      UserDetails creator = userDetailsService.loadUserByUsername(CREATOR);
      SecurityContext asCreator = SecurityContextHolder.createEmptyContext();
      asCreator.setAuthentication(
          UsernamePasswordAuthenticationToken.authenticated(
              creator, creator.getPassword(), creator.getAuthorities()));
      SecurityContextHolder.setContext(asCreator);
      assertions.run();
    } finally {
      SecurityContextHolder.setContext(original);
    }
  }

  /**
   * Follows one Transition of the pinned graph and waits for it to land. Refuses to follow an edge
   * the table does not claim, so a setup path can never quietly explore behaviour nothing pins.
   */
  private void drive(String negotiationId, String event) {
    String target =
        NegotiationGraphV1.target(adapter.currentNegotiationState(negotiationId), event);
    adapter.sendNegotiationEvent(negotiationId, event);
    awaitState(negotiationId, target);
  }

  private void awaitState(String negotiationId, String expected) {
    LifecyclePersistence.awaitState(expected, () -> adapter.currentNegotiationState(negotiationId));
  }
}
