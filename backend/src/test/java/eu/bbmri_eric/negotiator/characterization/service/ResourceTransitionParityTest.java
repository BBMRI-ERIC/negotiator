package eu.bbmri_eric.negotiator.characterization.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.bbmri_eric.negotiator.characterization.adapter.LifecycleTestAdapter;
import eu.bbmri_eric.negotiator.characterization.adapter.LifecycleTestAdapterConfig;
import eu.bbmri_eric.negotiator.common.configuration.security.oauth2.NegotiatorJwtAuthenticationToken;
import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.user.Person;
import eu.bbmri_eric.negotiator.util.IntegrationTest;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.annotation.DirtiesContext;

/**
 * Fires every Transition of the Resource Lifecycle Definition graph for real, and pins how the
 * service refuses the Events it will not fire.
 *
 * <p>Every State and Event is named as a string literal. Nothing here imports Spring Statemachine
 * or a Lifecycle enum, so these assertions can be re-run byte-identical against the replacement
 * subsystem - only {@link LifecycleTestAdapter}'s implementation changes. The table being driven is
 * {@link ResourceGraphV1}, which {@link ResourceGraphV1BindingTest} equates edge for edge to the
 * committed mechanical dump; this class supplies the other half of the argument, that the graph the
 * dump describes is the graph the service actually walks.
 *
 * <p><b>The subject.</b> {@code negotiation-1} is the only seeded Negotiation that is IN_PROGRESS
 * and carries a Resource in the graph's initial State. Its Resource is {@code
 * biobank:1:collection:1} (row id 4), whose representatives are 109 and 103, while the
 * Negotiation's creator is 108 and neither is an admin. That separates the three Required Authority
 * rules cleanly: no single caller can fire the whole chain, which is why the walk below changes
 * identity between steps.
 *
 * <p><b>The Resource identifier is the Resource's {@code source_id}, never its row id.</b> Both the
 * State lookup ({@code rl.id.resource.sourceId = :resourceId}) and the representative check ({@code
 * resource.getSourceId()}) key on it, and so does the persist listener that writes the new State
 * back. A test that passed "4" would silently find nothing and pass for the wrong reason.
 *
 * <p><b>Placing a Resource in a State.</b> Several Transitions start from States no seeded row is
 * in and that no single caller can reach unaided, so the table-driven test writes the starting
 * State straight onto the link row with SQL. That names the State as a string too, and it keeps
 * each row of the table an independent statement about one Transition rather than a step of one
 * long chain. The walk test covers the chain end to end using nothing but Events.
 *
 * <p><b>Asynchrony.</b> {@code sendEvent} drives the persist handler through {@code
 * handleEventWithStateReactively(...).subscribe()} and returns the State read back immediately -
 * that is, usually the State the Resource was already in. Every assertion about the resulting State
 * therefore polls with Awaitility under a bounded timeout, and no method here is transactional.
 *
 * <p>{@code @DirtiesContext} per method rebuilds the context, and with it Flyway's
 * clean-and-migrate of the seeded data, because these tests move shared seeded rows. That is the
 * rule ticket 03 left behind for this class: a Resource driven to a terminal State can conclude
 * {@code negotiation-1}, which {@code NegotiationAuthorityParityTest} reads expecting IN_PROGRESS,
 * so a driving class that did not restore the seed could turn ticket 03 red by test ordering alone.
 */
@IntegrationTest(loadTestData = true)
@Import(LifecycleTestAdapterConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ResourceTransitionParityTest {

  private static final String NEGOTIATION = "negotiation-1";

  /** The {@code source_id} of Resource row 4, which is what every Lifecycle path keys on. */
  private static final String RESOURCE = "biobank:1:collection:1";

  private static final long RESOURCE_ROW_ID = 4L;

  /** A Resource of the seed that {@code negotiation-1} has no link row for. */
  private static final String UNLINKED_RESOURCE = "biobank:3:collection:1";

  private static final long ADMIN = 101L;
  private static final long REPRESENTATIVE = 109L;
  private static final long CREATOR = 108L;

  /** Which seeded caller satisfies which of the graph's three Required Authority rules. */
  private static final Map<String, Long> CALLER_SATISFYING =
      Map.of(
          ResourceGraphV1.IS_ADMIN, ADMIN,
          ResourceGraphV1.IS_REPRESENTATIVE, REPRESENTATIVE,
          ResourceGraphV1.IS_CREATOR, CREATOR);

  private static final Duration PERSIST_TIMEOUT = Duration.ofSeconds(15);

  /** Long enough for the asynchronous persist path to have run had the Event been accepted. */
  private static final Duration SETTLE = Duration.ofSeconds(3);

  @Autowired LifecycleTestAdapter adapter;
  @Autowired JdbcTemplate jdbcTemplate;

  @AfterEach
  void clearAuthentication() {
    SecurityContextHolder.clearContext();
  }

  /**
   * The thirteen Transitions of the graph, taken from the table the dump is bound to rather than
   * retyped here, so this test cannot drift from the committed artifact.
   */
  static Stream<Arguments> transitions() {
    return ResourceGraphV1.TRANSITIONS.stream()
        .map(
            edge ->
                Arguments.of(
                    edge.source(),
                    edge.event(),
                    edge.target(),
                    CALLER_SATISFYING.get(edge.requiredAuthority())));
  }

  @ParameterizedTest(name = "{0} --{1}--> {2}")
  @MethodSource("transitions")
  @DisplayName("every Transition of the Resource graph moves the Resource to its target State")
  void transition_movesResourceToTargetState(
      String source, String event, String target, long caller) {
    putResourceInState(source);
    authenticateAs(caller);

    assertEquals(source, adapter.currentResourceState(NEGOTIATION, RESOURCE));
    assertTrue(
        adapter.possibleResourceEvents(NEGOTIATION, RESOURCE).contains(event),
        "%s should be offered from %s to caller %d".formatted(event, source, caller));

    adapter.sendResourceEvent(NEGOTIATION, RESOURCE, event);

    await()
        .atMost(PERSIST_TIMEOUT)
        .untilAsserted(
            () -> assertEquals(target, adapter.currentResourceState(NEGOTIATION, RESOURCE)));
  }

  /**
   * The same graph walked as one Lifecycle rather than thirteen independent statements, from the
   * seeded initial State to delivery, using nothing but Events. Each step's target is looked up in
   * the pinned table from the State the Resource is actually in, so the walk can never quietly
   * follow an edge the table does not claim.
   */
  @Test
  @DisplayName(
      "the delivery chain runs end to end from the initial State, changing caller at every gate")
  void deliveryChain_reachesResourceMadeAvailable() {
    assertEquals(
        ResourceGraphV1.INITIAL_STATE, adapter.currentResourceState(NEGOTIATION, RESOURCE));

    fire(ADMIN, "CONTACT");
    fire(REPRESENTATIVE, "MARK_AS_CHECKING_AVAILABILITY");
    fire(REPRESENTATIVE, "MARK_AS_AVAILABLE");
    fire(REPRESENTATIVE, "INDICATE_ACCESS_CONDITIONS");
    fire(CREATOR, "ACCEPT_ACCESS_CONDITIONS");
    fire(REPRESENTATIVE, "GRANT_ACCESS_TO_RESOURCE");

    assertEquals("RESOURCE_MADE_AVAILABLE", adapter.currentResourceState(NEGOTIATION, RESOURCE));
  }

  /**
   * The refusal asymmetry, pinned deliberately.
   *
   * <p>An Event that is not offered leaves the Resource Lifecycle service silent: it returns the
   * unchanged current State and raises nothing at all. The Negotiation Lifecycle service refuses
   * the same situation by throwing {@code ForbiddenRequestException}. Callers depend on that
   * difference today, so it is frozen here rather than smoothed over - making the two consistent
   * later must be a decision that breaks this test, not an accident nobody notices.
   *
   * <p>The {@code pollDelay} is what makes "silent" mean something: without it, a no-op and a
   * transition that simply had not landed yet would look the same. The State is read only after the
   * asynchronous persist path has had time to run.
   */
  @ParameterizedTest(name = "{0}")
  @MethodSource("refusals")
  @DisplayName(
      "an Event the caller may not fire returns the unchanged State and raises nothing"
          + " - unlike the Negotiation service, which throws")
  void unofferedEvent_returnsUnchangedStateAndRaisesNothing_unlikeTheNegotiationService(
      String shapeOfTheRefusal, long caller, String event) {
    authenticateAs(caller);
    assertEquals(
        ResourceGraphV1.INITIAL_STATE, adapter.currentResourceState(NEGOTIATION, RESOURCE));
    assertFalse(
        adapter.possibleResourceEvents(NEGOTIATION, RESOURCE).contains(event),
        "%s must not be offered here (%s), or this pins nothing"
            .formatted(event, shapeOfTheRefusal));

    assertThatCode(() -> assertEquals(ResourceGraphV1.INITIAL_STATE, send(event)))
        .doesNotThrowAnyException();

    awaitStillInTheInitialState();
  }

  static Stream<Arguments> refusals() {
    return Stream.of(
        Arguments.of("an Event this caller has no Required Authority for", CREATOR, "CONTACT"),
        Arguments.of(
            "an Event with no Transition from this State", ADMIN, "GRANT_ACCESS_TO_RESOURCE"));
  }

  /**
   * An Event that exists in the Definition but sits on no Transition is refused exactly like an
   * Event the caller has no authority for: silently. Both {@code RETURN_FOR_RESUBMISSION} and
   * {@code OVERRIDE} are declared only because the configuration registers the whole enum, and the
   * frozen dump records no Transition carrying either. The caller here is an admin who is being
   * offered Events at the same moment, so the refusal cannot be confused with the IN_PROGRESS gate.
   */
  @ParameterizedTest(name = "{0}")
  @MethodSource("eventsOnNoTransition")
  @DisplayName("an Event that sits on no Transition is refused silently, for an admin too")
  void eventOnNoTransition_isSilentlyRefused(String event) {
    authenticateAs(ADMIN);
    assertTrue(adapter.possibleResourceEvents(NEGOTIATION, RESOURCE).contains("CONTACT"));

    assertThatCode(() -> assertEquals(ResourceGraphV1.INITIAL_STATE, send(event)))
        .doesNotThrowAnyException();

    awaitStillInTheInitialState();
  }

  static Stream<Arguments> eventsOnNoTransition() {
    return ResourceGraphV1.EVENTS_ON_NO_TRANSITION.stream().sorted().map(Arguments::of);
  }

  /**
   * The one place the silent refusal is not silent.
   *
   * <p>"Returns the unchanged current State" needs a current State to return. The refusal path
   * reads it back unguarded, so when the link row records no State the read raises {@code
   * EntityNotFoundException} - carrying the Negotiation's id, not the Resource's, because that is
   * the argument the lookup was written with. So the asymmetry with the Negotiation service is
   * narrower than it looks: the Resource service is silent only where there is something to be
   * silent about.
   */
  @Test
  @DisplayName("refusing an Event for a linked Resource with no recorded State is not silent")
  void sendEvent_toALinkedResourceWithoutACurrentState_raisesEntityNotFound() {
    authenticateAs(ADMIN);
    clearResourceState();

    assertThatThrownBy(() -> adapter.sendResourceEvent(NEGOTIATION, RESOURCE, "CONTACT"))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessageContaining(NEGOTIATION);
  }

  /**
   * A Resource the Negotiation has no link row for at all fails identically - the State lookup
   * finds nothing either way, so "not linked" and "linked but blank" are indistinguishable to a
   * caller. The subject Resource is deliberately left alone here: it is not what is being sent to.
   */
  @Test
  @DisplayName("refusing an Event for a Resource not linked to the Negotiation is not silent")
  void sendEvent_toAResourceNotLinkedToTheNegotiation_raisesEntityNotFound() {
    authenticateAs(ADMIN);

    assertThatThrownBy(() -> adapter.sendResourceEvent(NEGOTIATION, UNLINKED_RESOURCE, "CONTACT"))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessageContaining(NEGOTIATION);
  }

  /**
   * Fires {@code event} as {@code caller} and waits for the State the pinned table says it leads
   * to, from wherever the Resource currently stands.
   */
  private void fire(long caller, String event) {
    String source = adapter.currentResourceState(NEGOTIATION, RESOURCE);
    String target = ResourceGraphV1.target(source, event);
    authenticateAs(caller);
    assertTrue(
        adapter.possibleResourceEvents(NEGOTIATION, RESOURCE).contains(event),
        "%s should be offered from %s to caller %d".formatted(event, source, caller));

    adapter.sendResourceEvent(NEGOTIATION, RESOURCE, event);

    await()
        .atMost(PERSIST_TIMEOUT)
        .untilAsserted(
            () -> assertEquals(target, adapter.currentResourceState(NEGOTIATION, RESOURCE)));
  }

  private void awaitStillInTheInitialState() {
    await()
        .pollDelay(SETTLE)
        .atMost(PERSIST_TIMEOUT)
        .untilAsserted(
            () ->
                assertEquals(
                    ResourceGraphV1.INITIAL_STATE,
                    adapter.currentResourceState(NEGOTIATION, RESOURCE)));
  }

  private String send(String event) {
    return adapter.sendResourceEvent(NEGOTIATION, RESOURCE, event);
  }

  /** Writes a starting State straight onto the link row, naming it as a string. */
  private void putResourceInState(String state) {
    jdbcTemplate.update(
        "update negotiation_resource_link set current_state = ?"
            + " where negotiation_id = ? and resource_id = ?",
        state,
        NEGOTIATION,
        RESOURCE_ROW_ID);
  }

  private void clearResourceState() {
    jdbcTemplate.update(
        "update negotiation_resource_link set current_state = null"
            + " where negotiation_id = ? and resource_id = ?",
        NEGOTIATION,
        RESOURCE_ROW_ID);
  }

  /**
   * Authenticates as a seeded Person, the way the production security filter would: a {@code
   * NegotiatorJwtAuthenticationToken} whose principal wraps the Person, which is what {@code
   * AuthenticatedUserContext} unwraps to an internal id.
   *
   * <p>Set programmatically rather than by annotation because the caller varies per row of a
   * table-driven test.
   */
  private void authenticateAs(long personId) {
    authenticateAs(personId, personId == ADMIN ? List.of("ROLE_ADMIN") : List.of());
  }

  private void authenticateAs(long personId, List<String> authorities) {
    Person principal = Person.builder().id(personId).name("caller-" + personId).build();
    Collection<GrantedAuthority> granted = new ArrayList<>();
    authorities.forEach(authority -> granted.add(new SimpleGrantedAuthority(authority)));
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(new NegotiatorJwtAuthenticationToken(principal, testJwt(), granted));
    SecurityContextHolder.setContext(context);
  }

  private static Jwt testJwt() {
    HashMap<String, Object> headers = new HashMap<>();
    headers.put("typ", "JWT");
    HashMap<String, Object> claims = new HashMap<>();
    claims.put("sub", "characterization");
    return new Jwt(
        "testToken", Instant.now(), Instant.now().plus(3L, ChronoUnit.HOURS), headers, claims);
  }
}
