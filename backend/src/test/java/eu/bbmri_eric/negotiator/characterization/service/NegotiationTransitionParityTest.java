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
 * <p>All eight Transitions are fired against the Lifecycle service and the resulting State is read
 * back, so a missing, misdirected or new Transition fails here. The rows come from {@link
 * NegotiationGraphV1}, copied from the dump.
 *
 * <p>The caller is always an admin, the one caller offered every Transition. {@link
 * NegotiationAuthorityParityTest} pins the narrower rules for the four seeded States, and this
 * class for the other three. Every State is read back under a bounded wait, and no method is
 * transactional, because Awaitility polls on a thread that cannot see the test's transaction.
 *
 * <p>{@code negotiation-2} is seeded {@code SUBMITTED} with no Resources, so firing at it does not
 * Spawn. Only the {@code DRAFT} row uses {@code negotiation-6}, the one Negotiation seeded there.
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
   * Each row is one Transition, with the Events that first bring the subject to its source State.
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
   * The three States no seeded Negotiation is in. Their Possible Events are pinned here because
   * reaching them means firing a Negotiation there first, and an empty set is how a terminal State
   * shows from outside.
   *
   * <p>Both callers use the same rows: no admin-only Transition leaves these States, so the creator
   * is offered exactly what the admin is, unlike in {@code SUBMITTED}.
   *
   * <p>The expectations come from {@link NegotiationGraphV1}, bound to the committed dump by {@link
   * NegotiationGraphV1BindingTest}.
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
   * The same three States as the creator sees them. Only an admin can move a Negotiation into
   * {@code PAUSED}, {@code DECLINED} or {@code CONCLUDED}.
   *
   * <p>So the test fires as an admin and reads as the creator, switching caller mid-method, which a
   * single {@code @WithUserDetails} cannot do. The switch builds the creator's {@code
   * Authentication} from the same {@code UserDetailsService} the annotation uses.
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
   * Runs {@code assertions} as the creator of {@link #NO_RESOURCES}, then restores the original
   * caller for the rest of the method and the {@code @DirtiesContext} teardown.
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
   * Follows one Transition of the graph and waits for it to land. It refuses an edge the table does
   * not list, so setup never relies on behaviour nothing pins.
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
