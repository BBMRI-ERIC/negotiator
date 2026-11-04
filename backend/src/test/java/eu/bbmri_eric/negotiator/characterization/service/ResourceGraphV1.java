package eu.bbmri_eric.negotiator.characterization.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The Resource Definition graph, named in strings.
 *
 * <p>Copied from the dump at {@code src/test/resources/lifecycle/resource-graph-v1.json}, not from
 * the configuration source. {@link ResourceGraphV1BindingTest} checks every constant against the
 * dump and the State and Event metadata, so a copying slip fails a test. {@link
 * ResourceTransitionParityTest} fires every row against the real Lifecycle, and {@link
 * ResourcePossibleEventsAuthorityTest} checks each State's Possible Events per kind of caller.
 *
 * <p>Three facts about this graph shape the tables below.
 *
 * <p>The Resource machine does not enable Spring Statemachine's security; {@code
 * ResourceLifecycleServiceImpl.isSecurityRuleMet} checks the three rule names below in code. Each of
 * the thirteen Transitions has exactly one rule, so the fourth column of {@link #TRANSITIONS} alone
 * decides who may fire it.
 *
 * <p>{@code RETURNED_FOR_RESUBMISSION} is a Legacy State. The configuration registers the whole
 * enum, so the Definition declares twelve States though no Transition names this one.
 *
 * <p>{@code RETURN_FOR_RESUBMISSION} and {@code OVERRIDE} are published but have no Transition, so
 * they are never offered. Tests send them to pin how an Event with no Transition is refused.
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
   * A rule name no Transition has and the service cannot satisfy, so a caller with none of the three
   * relationships fits in the same table.
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

  /** Declared by the Definition, named by no Transition; see the class comment. */
  static final String LEGACY_STATE = "RETURNED_FOR_RESUBMISSION";

  /**
   * The twelve States the Definition declares: the eleven the Transitions name, plus the Legacy
   * State no Transition mentions.
   *
   * <p>Derived rather than listed, so the graph is stated once. {@link
   * ResourceGraphV1BindingTest#allStateNames_areTheDumpsStatesAndThePublishedStates} checks it equals
   * the dump's {@code states} array and the published States. Unordered, because nothing pins an
   * order for it, unlike {@link #TRANSITIONS}.
   */
  static Set<String> allStateNames() {
    return Stream.concat(
            TRANSITIONS.stream().flatMap(edge -> Stream.of(edge.source(), edge.target())),
            Stream.of(LEGACY_STATE))
        .collect(Collectors.toUnmodifiableSet());
  }

  /**
   * Every Event the Definition publishes: thirteen names, two more than the dump's triggers. Taken
   * from the published metadata so the refusal tests cover every name a caller could send.
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
   * The State a Resource's parent Negotiation must be in before any Event is offered. The service
   * checks it in code, before looking at any Transition.
   */
  static final String REQUIRED_PARENT_STATE = "IN_PROGRESS";

  /**
   * The comparison type every security rule of the dump records.
   *
   * <p>The configuration asks for {@code ComparisonType.ALL} on all thirteen chains and the dump
   * reads back {@code ANY}, because Spring Statemachine 4.0.0 ignores the argument. Pinned as
   * observed, since that is the behaviour to reproduce. It is harmless only because every rule has
   * exactly one attribute.
   */
  static final String COMPARISON_TYPE = "ANY";

  /**
   * The Events offered from {@code state} to a caller satisfying {@code authorityRule} and no other
   * rule. {@link #NO_AUTHORITY} gives the empty set from every State.
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
   * @throws IllegalArgumentException when the graph has no such Transition, so a test cannot follow
   *     an edge this table does not list
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
