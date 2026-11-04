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
 * <p>The Resource machine does not enable Spring Statemachine's security. The service checks three
 * rule names in code, {@code isAdmin}, {@code isRepresentative} and {@code isCreator}, and offers
 * nothing unless the parent Negotiation is IN_PROGRESS. Everything is asserted through {@link
 * LifecycleTestAdapter} with States and Events as strings.
 *
 * <p>The expectations come from {@link ResourceGraphV1}, which {@link ResourceGraphV1BindingTest}
 * binds to the committed dump and metadata. The parent Negotiation States come from {@link
 * NegotiationGraphV1}.
 *
 * <p>No test fires an Event. The only writes are the test's own SQL placing the subject and its
 * parent in a State, and {@link #resetSubject()} undoes both before every method, so the context
 * is dirtied only after the class.
 *
 * <p>The subject and the three callers for the three Required Authority values come from {@link
 * SeededResourceSubject}. Two more callers appear only here: 104 has none of those relationships
 * and is not an admin, and 105 represents only Resources of another organization.
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
   * One assertion per caller: for each of the twelve declared States, the Events that caller is
   * offered.
   *
   * <p>Every Transition has exactly one Required Authority rule, so a caller is offered the
   * Transitions leaving the State whose rule they satisfy. A caller who satisfies none is offered
   * nothing anywhere, the fourth row. The four terminal States and the Legacy State offer nothing to
   * anybody.
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
   * The IN_PROGRESS gate, walked across every Negotiation State. The service checks it in code
   * before any Transition, so with the Negotiation in any other State an admin is offered nothing.
   *
   * <p>{@code NegotiationIsApprovedGuard} sits on no Transition (see {@link
   * ResourceGraphV1BindingTest#noTransition_carriesAGuard}) and never fires, so this gate is the
   * only parent check to pin.
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
   * A linked Resource with no recorded State looks like an unlinked one: the State lookup finds
   * nothing, the service swallows that, and the answer is empty, even for an admin. Sending an
   * Event here is the one case where the Resource service throws; see {@link
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
   * <p>The rule check ends with "an absent {@code Authentication} satisfies {@code isAdmin}", but
   * that branch is unreachable. Resolving the caller's internal id runs first and throws {@code
   * AuthenticationCredentialsNotFoundException}, which the rule check catches neither as a {@code
   * ClassCastException} nor as a {@code NullPointerException}. An unauthenticated caller sees this
   * exception, never an admin's Events.
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
   * <p>A principal that is not a Negotiator principal causes a {@code ClassCastException}, but
   * inside the caller resolution, which catches every exception and rethrows {@code
   * AuthenticationCredentialsNotFoundException}. The rule check's own {@code catch
   * (ClassCastException)} never runs. The Negotiation service has the same unreachable branch.
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
