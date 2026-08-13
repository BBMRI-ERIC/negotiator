package eu.bbmri_eric.negotiator.characterization.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The Negotiation Definition graph as it stands today, named entirely in strings.
 *
 * <p>Transcribed from the mechanically produced dump at {@code
 * src/test/resources/lifecycle/negotiation-graph-v1.json}, never from the configuration source by
 * eye. Nothing here is trusted on its own, and the transcription least of all:
 *
 * <ul>
 *   <li>{@link NegotiationGraphV1BindingTest} checks every constant below against the committed
 *       dump and against the committed Event metadata, so a slip of the hand fails a test rather
 *       than quietly redefining what the suite believes the graph to be;
 *   <li>{@link NegotiationTransitionParityTest} fires every row against the real Lifecycle, and
 *       {@link NegotiationAuthorityParityTest} pins each State's Possible Events against the same
 *       rows.
 * </ul>
 *
 * <p>So by the end of the suite this table is a model the service has agreed with rather than a
 * second hand-written statement of it.
 *
 * <p>Two facts about the graph are worth stating here because they shape every table below:
 *
 * <ul>
 *   <li>the Definition declares eight States but only seven can ever hold a Negotiation - {@code
 *       APPROVED} is a Legacy State, declared because the configuration registers the whole set at
 *       once, with no Transition leading into it and no seeded Negotiation sitting in it;
 *   <li>{@code DRAFT} is a source with no way in: {@code DRAFT --SUBMIT--> SUBMITTED} exists, no
 *       Transition anywhere targets {@code DRAFT}, and the initial State is {@code SUBMITTED}. That
 *       it is nevertheless *occupied* by seeded data is a separate fact, pinned by {@link
 *       NegotiationDraftReachabilityTest}.
 * </ul>
 */
final class NegotiationGraphV1 {

  /** One directed edge of the graph: from {@code source}, {@code event} leads to {@code target}. */
  record Edge(String source, String event, String target) {}

  /** The eight Transitions, in the order the dump lists them. */
  static final List<Edge> TRANSITIONS =
      List.of(
          new Edge("DRAFT", "SUBMIT", "SUBMITTED"),
          new Edge("IN_PROGRESS", "ABANDON", "ABANDONED"),
          new Edge("IN_PROGRESS", "CONCLUDE", "CONCLUDED"),
          new Edge("IN_PROGRESS", "PAUSE", "PAUSED"),
          new Edge("PAUSED", "ABANDON", "ABANDONED"),
          new Edge("PAUSED", "UNPAUSE", "IN_PROGRESS"),
          new Edge("SUBMITTED", "APPROVE", "IN_PROGRESS"),
          new Edge("SUBMITTED", "DECLINE", "DECLINED"));

  /** The Spring role a secured Transition of this graph names, as the dump spells it. */
  static final String ADMIN = "ROLE_ADMIN";

  /**
   * The two Transitions whose Required Authority is {@code ROLE_ADMIN}. The service keeps a
   * Transition whose rule is absent, or whose rule attributes intersect the caller's Spring roles,
   * so these two are the whole of the difference between an admin's Possible Events and a non-admin
   * creator's.
   */
  static final Set<String> ADMIN_ONLY_EVENTS = Set.of("APPROVE", "DECLINE");

  /** The State a Negotiation starts its Lifecycle in, per the dump's {@code initialState}. */
  static final String INITIAL_STATE = "SUBMITTED";

  /** Declared but unreachable and unoccupied - see the class comment. */
  static final String LEGACY_STATE = "APPROVED";

  /**
   * The Event universe the Definition publishes: eight names, one more than the dump lists.
   *
   * <p>This is the one constant here the dump does not settle. Its {@code events} array names only
   * the seven Events that actually trigger a Transition; {@code START} is a real Event all the same
   * - the metadata endpoint publishes it, a caller can name it - and it simply carries no
   * Transition, so it can never be offered from any State. Taking the universe from the published
   * metadata rather than from the dump is what lets the refusal coverage cover every name a caller
   * could send. {@link NegotiationGraphV1BindingTest} binds this field to {@code
   * characterization/rest/negotiation-events.json} and the dump's seven to the transitions that use
   * them, rather than pretending the two lists are the same list.
   */
  static final Set<String> ALL_EVENT_NAMES =
      Set.of("SUBMIT", "APPROVE", "DECLINE", "START", "PAUSE", "UNPAUSE", "ABANDON", "CONCLUDE");

  /** The Events an admin is offered from {@code state}: every Transition leaving it. */
  static Set<String> possibleEventsForAdmin(String state) {
    return TRANSITIONS.stream()
        .filter(edge -> edge.source().equals(state))
        .map(Edge::event)
        .collect(Collectors.toUnmodifiableSet());
  }

  /**
   * The Events the Negotiation's own creator is offered from {@code state}, assuming the creator
   * holds no admin role: the same Transitions, less the ones an admin alone may fire.
   */
  static Set<String> possibleEventsForCreator(String state) {
    return possibleEventsForAdmin(state).stream()
        .filter(event -> !ADMIN_ONLY_EVENTS.contains(event))
        .collect(Collectors.toUnmodifiableSet());
  }

  /** The Events that cannot be fired from {@code state} by anyone, admin included. */
  static Set<String> eventsNotOfferedFrom(String state) {
    Set<String> offered = possibleEventsForAdmin(state);
    return ALL_EVENT_NAMES.stream()
        .filter(event -> !offered.contains(event))
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

  /**
   * The Event labels the refusal message is built from, spelled out rather than derived, so that a
   * label edit shows up as a failing assertion instead of being absorbed by a clever expression.
   * Today every label is its Event name in title case, which is exactly the coincidence a
   * characterization suite should not lean on. Bound to the published labels by {@link
   * NegotiationGraphV1BindingTest}.
   */
  private static final Map<String, String> EVENT_LABELS =
      Map.of(
          "SUBMIT", "Submit",
          "APPROVE", "Approve",
          "DECLINE", "Decline",
          "START", "Start",
          "PAUSE", "Pause",
          "UNPAUSE", "Unpause",
          "ABANDON", "Abandon",
          "CONCLUDE", "Conclude");

  /**
   * The user-visible refusal the Negotiation service raises for {@code event}, built from that
   * Event's own label lowercased. Pinned because it reaches the frontend verbatim.
   */
  static String refusalMessage(String event) {
    return "You are not allowed to %s the Negotiation"
        .formatted(EVENT_LABELS.get(event).toLowerCase());
  }

  private NegotiationGraphV1() {}
}
