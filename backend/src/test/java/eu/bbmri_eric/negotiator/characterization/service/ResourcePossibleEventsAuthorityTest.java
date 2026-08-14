package eu.bbmri_eric.negotiator.characterization.service;

import static eu.bbmri_eric.negotiator.characterization.service.SeededResourceSubject.ADMIN;
import static eu.bbmri_eric.negotiator.characterization.service.SeededResourceSubject.CREATOR;
import static eu.bbmri_eric.negotiator.characterization.service.SeededResourceSubject.NEGOTIATION;
import static eu.bbmri_eric.negotiator.characterization.service.SeededResourceSubject.REPRESENTATIVE;
import static eu.bbmri_eric.negotiator.characterization.service.SeededResourceSubject.RESOURCE;
import static eu.bbmri_eric.negotiator.characterization.service.SeededResourceSubject.authenticateAs;
import static eu.bbmri_eric.negotiator.characterization.service.SeededResourceSubject.clearResourceState;
import static eu.bbmri_eric.negotiator.characterization.service.SeededResourceSubject.putNegotiationInState;
import static eu.bbmri_eric.negotiator.characterization.service.SeededResourceSubject.putResourceInState;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import eu.bbmri_eric.negotiator.characterization.adapter.LifecycleTestAdapter;
import eu.bbmri_eric.negotiator.characterization.adapter.LifecycleTestAdapterConfig;
import eu.bbmri_eric.negotiator.util.IntegrationTest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;

/**
 * Pins which Events the Resource Lifecycle offers, to whom, and when it offers none at all.
 *
 * <p>The Resource machine does not enable Spring Statemachine's security. Its service reimplements
 * rule evaluation imperatively against three rule names - {@code isAdmin}, {@code isRepresentative}
 * and {@code isCreator} - and gates the whole answer on the parent Negotiation being IN_PROGRESS.
 * Both are frozen here as behaviour, never as implementation: everything is asserted through {@link
 * LifecycleTestAdapter} with States and Events named as strings.
 *
 * <p>The expected offerings are not written out by hand. They are computed from {@link
 * ResourceGraphV1}, whose every constant {@link ResourceGraphV1BindingTest} equates to the
 * committed mechanical dump and to the committed metadata - so a passing assertion here is a
 * statement about the system rather than about a table this package happens to agree with. The
 * universe of parent Negotiation States comes from {@link NegotiationGraphV1} for the same reason,
 * rather than being transcribed a second time.
 *
 * <p><b>Why no per-method context rebuild.</b> Nothing here fires an Event, so nothing here moves a
 * Lifecycle. The only writes are this test's own SQL, which places the subject in the State or
 * parent State under examination, and {@link #resetSubject()} puts both back before every method.
 * The context is dirtied after the class, so no mutation escapes to another test class. (The rule
 * ticket 03 left behind - a class that <em>drives</em> a Lifecycle must dirty the context after
 * each test method - is met by {@link ResourceTransitionParityTest}, which is the driving class.)
 *
 * <p>The subject, and the three callers that satisfy the graph's three Required Authority rules,
 * are {@link SeededResourceSubject}. Two more callers appear only here: 104 has none of those
 * relationships and is not an admin, and 105 represents Resources of another organization only.
 */
@IntegrationTest(loadTestData = true)
@Import(LifecycleTestAdapterConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ResourcePossibleEventsAuthorityTest {

  /** ABANDONED in the seed, holding Resource {@code biobank:3:collection:1} in SUBMITTED. */
  private static final String ABANDONED_NEGOTIATION = "negotiation-v2";

  private static final String RESOURCE_OF_ANOTHER_NEGOTIATION = "biobank:3:collection:1";

  private static final long UNRELATED = 104L;

  /** Represents {@code biobank:3:*} and so no Resource of the subject Negotiation. */
  private static final long REPRESENTATIVE_OF_ANOTHER_RESOURCE = 105L;

  @Autowired LifecycleTestAdapter adapter;
  @Autowired JdbcTemplate jdbcTemplate;

  @BeforeEach
  void resetSubject() {
    putNegotiationInState(jdbcTemplate, ResourceGraphV1.REQUIRED_PARENT_STATE);
    putResourceInState(jdbcTemplate, ResourceGraphV1.INITIAL_STATE);
  }

  @AfterEach
  void clearAuthentication() {
    SecurityContextHolder.clearContext();
  }

  /**
   * The whole authority table in one assertion per caller: for each of the twelve declared States,
   * the Events that caller is offered.
   *
   * <p>Every Transition carries exactly one Required Authority rule, so what a caller is offered
   * from a State is exactly the Transitions leaving it whose rule that caller satisfies - and a
   * caller who satisfies none of the three is offered nothing anywhere, which is the fourth row.
   * Both halves are computed from the bound table, including the four terminal States and the
   * Legacy State, which offer nothing to anybody.
   */
  @ParameterizedTest(name = "{0}")
  @MethodSource("callersAndTheAuthorityRuleTheySatisfy")
  @DisplayName("the Events offered from every State, per kind of caller")
  void possibleEvents_perStatePerCaller(
      String callerDescription, long caller, String authorityRule) {
    authenticateAs(caller);

    Map<String, Set<String>> observed = new LinkedHashMap<>();
    Map<String, Set<String>> expected = new LinkedHashMap<>();
    for (String state : ResourceGraphV1.allStateNames().stream().sorted().toList()) {
      putResourceInState(jdbcTemplate, state);
      observed.put(state, adapter.possibleResourceEvents(NEGOTIATION, RESOURCE));
      expected.put(state, ResourceGraphV1.eventsFor(state, authorityRule));
    }

    assertThat(observed).containsExactlyInAnyOrderEntriesOf(expected);
  }

  static Stream<Arguments> callersAndTheAuthorityRuleTheySatisfy() {
    return Stream.of(
        Arguments.of("an admin", ADMIN, ResourceGraphV1.IS_ADMIN),
        Arguments.of(
            "a representative of the Resource", REPRESENTATIVE, ResourceGraphV1.IS_REPRESENTATIVE),
        Arguments.of("the Negotiation's creator", CREATOR, ResourceGraphV1.IS_CREATOR),
        Arguments.of(
            "a caller with none of those relationships", UNRELATED, ResourceGraphV1.NO_AUTHORITY),
        Arguments.of(
            "a representative of some other Resource",
            REPRESENTATIVE_OF_ANOTHER_RESOURCE,
            ResourceGraphV1.NO_AUTHORITY));
  }

  /**
   * The IN_PROGRESS gate, from both sides, walked across every State the Negotiation Definition
   * declares. It is imperative, in the service, and it answers before any Transition of the
   * Definition is consulted - so an admin standing on the initial State of a Resource whose
   * Negotiation is in any other State is offered nothing at all.
   *
   * <p>This is also where the {@code NegotiationIsApprovedGuard} question lands. That Guard is
   * attached to no Transition - see {@link ResourceGraphV1BindingTest#noTransition_carriesAGuard} -
   * so it never fires, and the parent ticket's requirement to pin "every guard outcome" is
   * satisfied by pinning this gate instead.
   */
  @ParameterizedTest(name = "parent in {0}")
  @MethodSource("parentStates")
  @DisplayName("Events are offered only while the parent Negotiation is IN_PROGRESS")
  void possibleEvents_areGatedOnTheParentBeingInProgress(String parentState) {
    authenticateAs(ADMIN);
    putNegotiationInState(jdbcTemplate, parentState);

    Set<String> offered = adapter.possibleResourceEvents(NEGOTIATION, RESOURCE);

    if (ResourceGraphV1.REQUIRED_PARENT_STATE.equals(parentState)) {
      assertThat(offered)
          .containsExactlyInAnyOrderElementsOf(
              ResourceGraphV1.eventsFor(ResourceGraphV1.INITIAL_STATE, ResourceGraphV1.IS_ADMIN))
          .isNotEmpty();
    } else {
      assertThat(offered).isEmpty();
    }
  }

  static Stream<Arguments> parentStates() {
    return NegotiationGraphV1.allStateNames().stream().sorted().map(Arguments::of);
  }

  @Test
  @DisplayName("a seeded ABANDONED Negotiation offers nothing for its SUBMITTED Resource")
  void possibleEvents_onSeededAbandonedNegotiation_isEmpty() {
    authenticateAs(ADMIN);

    assertThat(adapter.currentResourceState(ABANDONED_NEGOTIATION, RESOURCE_OF_ANOTHER_NEGOTIATION))
        .isEqualTo(ResourceGraphV1.INITIAL_STATE);
    assertThat(
            adapter.possibleResourceEvents(ABANDONED_NEGOTIATION, RESOURCE_OF_ANOTHER_NEGOTIATION))
        .isEmpty();
  }

  @Test
  @DisplayName("a Resource that is not linked to the Negotiation is offered nothing")
  void possibleEvents_forResourceNotLinkedToTheNegotiation_isEmpty() {
    authenticateAs(ADMIN);

    assertThat(adapter.currentResourceState(NEGOTIATION, RESOURCE_OF_ANOTHER_NEGOTIATION)).isNull();
    assertThat(adapter.possibleResourceEvents(NEGOTIATION, RESOURCE_OF_ANOTHER_NEGOTIATION))
        .isEmpty();
  }

  @Test
  @DisplayName("an unknown Negotiation offers nothing rather than reporting it does not exist")
  void possibleEvents_forUnknownNegotiation_isEmpty() {
    authenticateAs(ADMIN);

    assertThat(adapter.possibleResourceEvents("no-such-negotiation", RESOURCE)).isEmpty();
  }

  /**
   * A linked Resource with no recorded State is indistinguishable from an unlinked one here: the
   * State lookup yields nothing either way, the service swallows that, and the answer is an empty
   * set - even for an admin whose Negotiation is IN_PROGRESS. Sending an Event in that situation is
   * the one case where the Resource service is not silent; see {@link
   * ResourceTransitionParityTest}.
   */
  @Test
  @DisplayName("a linked Resource with no recorded State is offered nothing")
  void possibleEvents_forResourceWithNoRecordedState_isEmpty() {
    authenticateAs(ADMIN);
    clearResourceState(jdbcTemplate);

    assertThat(adapter.currentResourceState(NEGOTIATION, RESOURCE)).isNull();
    assertThat(adapter.possibleResourceEvents(NEGOTIATION, RESOURCE)).isEmpty();
  }

  /**
   * What a caller with no {@code Authentication} actually observes.
   *
   * <p>The service's rule evaluation ends with "an absent {@code Authentication} satisfies {@code
   * isAdmin}", and that branch is unreachable. Resolving the caller's internal id happens first,
   * and with no {@code Authentication} in the context the resolution wraps its own failure in
   * {@code AuthenticationCredentialsNotFoundException} - which the rule evaluation catches neither
   * as a {@code ClassCastException} nor as a {@code NullPointerException}, so it escapes the
   * service whole. An unauthenticated caller sees this exception, never an admin's Events.
   *
   * <p>Frozen as the observable behaviour, because that is what the replacement subsystem has to
   * reproduce - not what the branch appears to promise.
   */
  @Test
  @DisplayName("with no Authentication at all, resolving the caller fails before any rule is met")
  void possibleEvents_withoutAuthentication_raisesCredentialsNotFound() {
    SecurityContextHolder.clearContext();

    assertThatThrownBy(() -> adapter.possibleResourceEvents(NEGOTIATION, RESOURCE))
        .isInstanceOf(AuthenticationCredentialsNotFoundException.class)
        .hasMessageContaining("No authenticated user found");
  }

  /**
   * The {@code ClassCastException}-returns-false branch, pinned by its observable outcome.
   *
   * <p>A principal that is not a Negotiator principal does provoke a {@code ClassCastException} -
   * but inside the caller resolution, which catches every exception and rethrows {@code
   * AuthenticationCredentialsNotFoundException}. The rule evaluation's own {@code catch
   * (ClassCastException)} therefore never runs, and the caller sees the same failure as an
   * unauthenticated one. Ticket 03 found the identical dead branch in the Negotiation service.
   */
  @Test
  @DisplayName("a principal that is not a Negotiator principal fails the same way")
  void possibleEvents_withForeignPrincipal_raisesCredentialsNotFound() {
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(
        new UsernamePasswordAuthenticationToken("not-a-negotiator-principal", null, List.of()));
    SecurityContextHolder.setContext(context);

    assertThatThrownBy(() -> adapter.possibleResourceEvents(NEGOTIATION, RESOURCE))
        .isInstanceOf(AuthenticationCredentialsNotFoundException.class)
        .hasMessageContaining("No authenticated user found");
  }

  /**
   * Being an admin is decided by the {@code ROLE_ADMIN} authority on the token, not by the {@code
   * admin} column of the seeded Person row: caller 101 without that authority is offered nothing.
   */
  @ParameterizedTest(name = "authority {0}")
  @ValueSource(strings = {"ROLE_ADMIN", "ROLE_REPRESENTATIVE_biobank:1:collection:1", "RESEARCHER"})
  @DisplayName("the admin rule is met by the ROLE_ADMIN authority and by nothing else")
  void adminRule_isMetOnlyByTheRoleAdminAuthority(String authority) {
    authenticateAs(ADMIN, List.of(authority));

    Set<String> offered = adapter.possibleResourceEvents(NEGOTIATION, RESOURCE);

    if ("ROLE_ADMIN".equals(authority)) {
      assertThat(offered)
          .containsExactlyInAnyOrderElementsOf(
              ResourceGraphV1.eventsFor(ResourceGraphV1.INITIAL_STATE, ResourceGraphV1.IS_ADMIN));
    } else {
      assertThat(offered).isEmpty();
    }
  }
}
