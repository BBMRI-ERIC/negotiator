package eu.bbmri_eric.negotiator.characterization.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The Resource Definition graph as it stands today, named entirely in strings.
 *
 * <p>Transcribed from the mechanically produced dump at {@code
 * src/test/resources/lifecycle/resource-graph-v1.json}, never from the configuration source by eye,
 * and - following the lesson ticket 03 recorded - nothing here is trusted on its own:
 *
 * <ul>
 *   <li>{@link ResourceGraphV1BindingTest} checks every constant below against the committed dump
 *       and against the committed State and Event metadata, so a slip of the hand fails a test
 *       rather than quietly redefining what the suite believes the graph to be;
 *   <li>{@link ResourceTransitionParityTest} fires every row against the real Lifecycle, and {@link
 *       ResourcePossibleEventsAuthorityTest} pins each State's Possible Events, per kind of caller,
 *       against the same rows.
 * </ul>
 *
 * <p>Three facts about this graph shape every table below.
 *
 * <p><b>Required Authority is per Transition, and it is evaluated imperatively.</b> Unlike the
 * Negotiation machine, the Resource machine does not enable Spring Statemachine's security at all;
 * {@code ResourceLifecycleServiceImpl.isSecurityRuleMet} reimplements rule evaluation against the
 * three rule names below. Every one of the thirteen Transitions carries exactly one rule, so a
 * Transition is offered to precisely one kind of caller and the fourth column of {@link
 * #TRANSITIONS} fully determines who may fire it.
 *
 * <p><b>{@code RETURNED_FOR_RESUBMISSION} is a Legacy State.</b> The configuration registers the
 * whole enum, so the Definition declares twelve States while no Transition anywhere names this one.
 *
 * <p><b>The Event universe is two wider than the dump.</b> {@code RETURN_FOR_RESUBMISSION} and
 * {@code OVERRIDE} are declared and published, and carry no Transition, so they can never be
 * offered from any State - which is exactly what makes them worth sending at the service to pin how
 * a Transition-less Event is refused.
 */
final class ResourceGraphV1 {

  /**
   * One directed edge of the graph: from {@code source}, {@code event} leads to {@code target}, and
   * only a caller satisfying {@code requiredAuthority} is offered it.
   */
  record Edge(String source, String event, String target, String requiredAuthority) {}

  /** The rule name a Transition reserved for administrators carries, as the dump spells it. */
  static final String IS_ADMIN = "isAdmin";

  /** The rule name a Transition reserved for a representative of the Resource carries. */
  static final String IS_REPRESENTATIVE = "isRepresentative";

  /** The rule name a Transition reserved for the parent Negotiation's creator carries. */
  static final String IS_CREATOR = "isCreator";

  /** The three rule names the service knows how to evaluate. */
  static final List<String> ALL_AUTHORITY_RULES = List.of(IS_ADMIN, IS_REPRESENTATIVE, IS_CREATOR);

  /**
   * A rule name no Transition carries and the service cannot satisfy, used to express "a caller
   * with none of the three relationships" as a row of the same table.
   */
  static final String NO_AUTHORITY = "none";

  /** The thirteen Transitions, in the order the dump lists them. */
  static final List<Edge> TRANSITIONS =
      List.of(
          new Edge(
              "ACCESS_CONDITIONS_INDICATED",
              "ACCEPT_ACCESS_CONDITIONS",
              "ACCESS_CONDITIONS_MET",
              IS_CREATOR),
          new Edge(
              "ACCESS_CONDITIONS_INDICATED",
              "DECLINE_ACCESS_CONDITIONS",
              "RESOURCE_NOT_MADE_AVAILABLE",
              IS_CREATOR),
          new Edge(
              "ACCESS_CONDITIONS_MET",
              "GRANT_ACCESS_TO_RESOURCE",
              "RESOURCE_MADE_AVAILABLE",
              IS_REPRESENTATIVE),
          new Edge(
              "CHECKING_AVAILABILITY",
              "MARK_AS_AVAILABLE",
              "RESOURCE_AVAILABLE",
              IS_REPRESENTATIVE),
          new Edge(
              "CHECKING_AVAILABILITY",
              "MARK_AS_CURRENTLY_UNAVAILABLE_BUT_WILLING_TO_COLLECT",
              "RESOURCE_UNAVAILABLE_WILLING_TO_COLLECT",
              IS_REPRESENTATIVE),
          new Edge(
              "CHECKING_AVAILABILITY",
              "MARK_AS_UNAVAILABLE",
              "RESOURCE_UNAVAILABLE",
              IS_REPRESENTATIVE),
          new Edge(
              "REPRESENTATIVE_CONTACTED",
              "MARK_AS_CHECKING_AVAILABILITY",
              "CHECKING_AVAILABILITY",
              IS_REPRESENTATIVE),
          new Edge(
              "REPRESENTATIVE_CONTACTED", "STEP_AWAY", "RESOURCE_UNAVAILABLE", IS_REPRESENTATIVE),
          new Edge("REPRESENTATIVE_UNREACHABLE", "CONTACT", "REPRESENTATIVE_CONTACTED", IS_ADMIN),
          new Edge(
              "RESOURCE_AVAILABLE",
              "INDICATE_ACCESS_CONDITIONS",
              "ACCESS_CONDITIONS_INDICATED",
              IS_REPRESENTATIVE),
          new Edge(
              "RESOURCE_UNAVAILABLE_WILLING_TO_COLLECT",
              "INDICATE_ACCESS_CONDITIONS",
              "ACCESS_CONDITIONS_INDICATED",
              IS_REPRESENTATIVE),
          new Edge("SUBMITTED", "CONTACT", "REPRESENTATIVE_CONTACTED", IS_ADMIN),
          new Edge("SUBMITTED", "MARK_AS_UNREACHABLE", "REPRESENTATIVE_UNREACHABLE", IS_ADMIN));

  /** The State a Resource starts its Lifecycle in, per the dump's {@code initialState}. */
  static final String INITIAL_STATE = "SUBMITTED";

  /** Declared by the Definition, named by no Transition - see the class comment. */
  static final String LEGACY_STATE = "RETURNED_FOR_RESUBMISSION";

  /**
   * The twelve States the Definition declares: the eleven the Transitions name, plus the Legacy
   * State no Transition mentions.
   *
   * <p>Derived rather than listed, for the reason its sibling {@link
   * NegotiationGraphV1#allStateNames()} gives - so there is one statement of the graph and not two.
   * {@link ResourceGraphV1BindingTest#allStateNames_areTheDumpsStatesAndThePublishedStates} is what
   * makes the derivation safe: it equates exactly this set to the dump's {@code states} array and
   * to the States the metadata endpoint publishes. Unordered, because nothing pins an order for it
   * - unlike {@link #TRANSITIONS}, whose order the binding test does pin.
   */
  static Set<String> allStateNames() {
    return Stream.concat(
            TRANSITIONS.stream().flatMap(edge -> Stream.of(edge.source(), edge.target())),
            Stream.of(LEGACY_STATE))
        .collect(Collectors.toUnmodifiableSet());
  }

  /**
   * The Event universe the Definition publishes: thirteen names, two more than the dump's triggers.
   * Taken from the published metadata rather than from the dump, so that refusal coverage can cover
   * every name a caller could actually send.
   */
  static final Set<String> ALL_EVENT_NAMES =
      Set.of(
          "CONTACT",
          "MARK_AS_UNREACHABLE",
          "RETURN_FOR_RESUBMISSION",
          "MARK_AS_CHECKING_AVAILABILITY",
          "MARK_AS_AVAILABLE",
          "MARK_AS_UNAVAILABLE",
          "MARK_AS_CURRENTLY_UNAVAILABLE_BUT_WILLING_TO_COLLECT",
          "STEP_AWAY",
          "INDICATE_ACCESS_CONDITIONS",
          "ACCEPT_ACCESS_CONDITIONS",
          "DECLINE_ACCESS_CONDITIONS",
          "GRANT_ACCESS_TO_RESOURCE",
          "OVERRIDE");

  /** The two published Events that carry no Transition and can therefore never be offered. */
  static final Set<String> EVENTS_ON_NO_TRANSITION = Set.of("RETURN_FOR_RESUBMISSION", "OVERRIDE");

  /**
   * The State a Resource's parent Negotiation must be in before any Event is offered at all. The
   * check is imperative, in the service, and it answers before any Transition is consulted.
   */
  static final String REQUIRED_PARENT_STATE = "IN_PROGRESS";

  /**
   * The comparison type every security rule of the dump records.
   *
   * <p>The configuration asks for {@code ComparisonType.ALL} on all thirteen chains and the dump
   * reads back {@code ANY} on all thirteen, because Spring Statemachine 4.0.0 ignores the argument.
   * Pinned as the observed value, since that - not the builder chain - is what the replacement has
   * to reproduce. Harmless today only because every rule carries exactly one attribute.
   */
  static final String COMPARISON_TYPE = "ANY";

  /**
   * The Events a caller satisfying {@code authorityRule} - and no other rule - is offered from
   * {@code state}. {@link #NO_AUTHORITY} yields the empty set from every State, which is the whole
   * of what a caller with none of the three relationships is offered.
   */
  static Set<String> eventsFor(String state, String authorityRule) {
    return TRANSITIONS.stream()
        .filter(edge -> edge.source().equals(state))
        .filter(edge -> edge.requiredAuthority().equals(authorityRule))
        .map(Edge::event)
        .collect(Collectors.toUnmodifiableSet());
  }

  /** Every Event secured by {@code authorityRule}, wherever it appears in the graph. */
  static Set<String> eventsSecuredBy(String authorityRule) {
    return TRANSITIONS.stream()
        .filter(edge -> edge.requiredAuthority().equals(authorityRule))
        .map(Edge::event)
        .collect(Collectors.toUnmodifiableSet());
  }

  /**
   * Where {@code event} leads from {@code state}.
   *
   * @throws IllegalArgumentException when the graph has no such Transition, so a test that drives a
   *     Lifecycle through a path can never silently follow an edge this table does not claim
   */
  static String target(String state, String event) {
    return TRANSITIONS.stream()
        .filter(edge -> edge.source().equals(state) && edge.event().equals(event))
        .map(Edge::target)
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "The pinned graph has no Transition '%s' from '%s'".formatted(event, state)));
  }

  private ResourceGraphV1() {}
}
