package eu.bbmri_eric.negotiator.characterization.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The Negotiation Definition graph, named in strings.
 *
 * <p>Copied from the dump at {@code src/test/resources/lifecycle/negotiation-graph-v1.json}, not
 * from the configuration source. {@link NegotiationGraphV1BindingTest} checks every constant against
 * the dump and the Event metadata, so a copying slip fails a test. {@link
 * NegotiationTransitionParityTest} fires every row against the real Lifecycle, and {@link
 * NegotiationAuthorityParityTest} checks each State's Possible Events against the same rows.
 *
 * <p>Two facts about the graph shape the tables below:
 *
 * <ul>
 *   <li>The Definition declares eight States, and {@code APPROVED} is a Legacy State: the
 *       configuration registers the whole enum, no Transition enters it, and no seeded Negotiation
 *       is in it. Nothing here pins that a Negotiation could not be put in it from outside the
 *       Lifecycle, the way {@code DRAFT} is.
 *   <li>{@code DRAFT} has no way in: {@code DRAFT --SUBMIT--> SUBMITTED} exists, no Transition
 *       targets {@code DRAFT}, and the initial State is {@code SUBMITTED}. Seeded data still holds
 *       it; {@link NegotiationDraftReachabilityTest} pins that.
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

  /**
   * What a Transition does to the Negotiation's two post flags, named by the effect rather than the
   * bean that does it.
   *
   * <p>The dump names an Action by its bean class, and a redesign may rename those classes, so a
   * parity assertion must not name one. {@link NegotiationGraphV1BindingTest} maps bean names to
   * effects next to the dump; everything else speaks only of effects.
   */
  enum PostEffect {
    /** Public posts become possible on this Negotiation. */
    ENABLE_PUBLIC_POSTS,
    /** Private posts become possible on this Negotiation. */
    ENABLE_PRIVATE_POSTS,
    /** Both kinds of post stop being possible. One effect, because one Action does both. */
    DISABLE_POSTS
  }

  /** The two flags a {@link PostEffect} moves, as a caller can read them back. */
  record PostFlags(boolean publicPostsEnabled, boolean privatePostsEnabled) {

    /**
     * These flags after applying {@code effects}. No Transition has more than one effect ({@link
     * NegotiationGraphV1BindingTest} pins that), so the order does not matter.
     */
    PostFlags after(Set<PostEffect> effects) {
      boolean publicPosts = publicPostsEnabled;
      boolean privatePosts = privatePostsEnabled;
      for (PostEffect effect : effects) {
        switch (effect) {
          case ENABLE_PUBLIC_POSTS -> publicPosts = true;
          case ENABLE_PRIVATE_POSTS -> privatePosts = true;
          case DISABLE_POSTS -> {
            publicPosts = false;
            privatePosts = false;
          }
        }
      }
      return new PostFlags(publicPosts, privatePosts);
    }
  }

  /**
   * The post effect of every Transition, including the five that have none.
   *
   * <p>Lists all eight edges because the empty entries matter: {@code PAUSED --ABANDON-->
   * ABANDONED} has no effect while {@code IN_PROGRESS --ABANDON--> ABANDONED} disables both kinds of
   * post. {@link NegotiationGraphV1BindingTest} binds it to the dump's {@code actions} arrays and
   * {@link NegotiationPostEffectsTest} fires it.
   */
  static final Map<Edge, Set<PostEffect>> POST_EFFECTS =
      Map.ofEntries(
          Map.entry(
              new Edge("DRAFT", "SUBMIT", "SUBMITTED"), Set.of(PostEffect.ENABLE_PUBLIC_POSTS)),
          Map.entry(
              new Edge("IN_PROGRESS", "ABANDON", "ABANDONED"), Set.of(PostEffect.DISABLE_POSTS)),
          Map.entry(new Edge("IN_PROGRESS", "CONCLUDE", "CONCLUDED"), Set.of()),
          Map.entry(new Edge("IN_PROGRESS", "PAUSE", "PAUSED"), Set.of()),
          Map.entry(new Edge("PAUSED", "ABANDON", "ABANDONED"), Set.of()),
          Map.entry(new Edge("PAUSED", "UNPAUSE", "IN_PROGRESS"), Set.of()),
          Map.entry(
              new Edge("SUBMITTED", "APPROVE", "IN_PROGRESS"),
              Set.of(PostEffect.ENABLE_PRIVATE_POSTS)),
          Map.entry(new Edge("SUBMITTED", "DECLINE", "DECLINED"), Set.of()));

  /**
   * The post effect of firing {@code event} from {@code state}.
   *
   * @throws IllegalArgumentException when the graph has no such Transition, for the same reason
   *     {@link #target} does
   */
  static Set<PostEffect> postEffects(String state, String event) {
    Edge edge = new Edge(state, event, target(state, event));
    Set<PostEffect> effects = POST_EFFECTS.get(edge);
    if (effects == null) {
      throw new IllegalArgumentException("The pinned graph records no post effect for " + edge);
    }
    return effects;
  }

  /** The Spring role a secured Transition of this graph names, as the dump spells it. */
  static final String ADMIN = "ROLE_ADMIN";

  /**
   * The two Transitions whose Required Authority is {@code ROLE_ADMIN}. The service offers a
   * Transition with no rule, or whose rule attributes intersect the caller's Spring roles, so these
   * two are the only difference between an admin's Possible Events and a non-admin creator's.
   */
  static final Set<String> ADMIN_ONLY_EVENTS = Set.of("APPROVE", "DECLINE");

  /** The State a Negotiation starts its Lifecycle in, per the dump's {@code initialState}. */
  static final String INITIAL_STATE = "SUBMITTED";

  /** Declared but unreachable and unoccupied; see the class comment. */
  static final String LEGACY_STATE = "APPROVED";

  /**
   * Every Event the Definition publishes: eight names, one more than the dump lists.
   *
   * <p>The dump's {@code events} array names only the seven Events that trigger a Transition. The
   * metadata endpoint also publishes {@code START}, which has no Transition and so is never offered.
   * Taking the list from the published metadata lets the refusal tests cover every name a caller
   * could send. {@link NegotiationGraphV1BindingTest} binds this field to {@code
   * characterization/rest/negotiation-events.json} and the dump's seven to their Transitions.
   */
  static final Set<String> ALL_EVENT_NAMES =
      Set.of("SUBMIT", "APPROVE", "DECLINE", "START", "PAUSE", "UNPAUSE", "ABANDON", "CONCLUDE");

  /**
   * The eight States the Definition declares: the seven the Transitions name, plus the Legacy State
   * no Transition mentions.
   *
   * <p>Derived rather than listed, so the graph is stated once. {@link
   * NegotiationGraphV1BindingTest#statesNamedByTheTable_areDeclaredEverywhere} checks it equals the
   * dump's {@code states} array and the States the metadata endpoint publishes.
   *
   * <p>{@link ResourcePossibleEventsAuthorityTest} walks the Resource graph's IN_PROGRESS gate across
   * every Negotiation State and takes them from here.
   */
  static Set<String> allStateNames() {
    return Stream.concat(
            TRANSITIONS.stream().flatMap(edge -> Stream.of(edge.source(), edge.target())),
            Stream.of(LEGACY_STATE))
        .collect(Collectors.toUnmodifiableSet());
  }

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

  /**
   * The Event labels the refusal message uses, written out so a label edit fails an assertion. Every
   * label happens to be its Event name in title case, and the tests should not rely on that. {@link
   * NegotiationGraphV1BindingTest} binds them to the published labels.
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
   * Event's label in lower case. It reaches the frontend verbatim.
   */
  static String refusalMessage(String event) {
    return "You are not allowed to %s the Negotiation"
        .formatted(EVENT_LABELS.get(event).toLowerCase());
  }

  /**
   * The user-visible conflict the Negotiation service raises for {@code event} when no Transition
   * leaves the current State on it. It names the State by {@code stateLabel} and the Event by its
   * label.
   */
  static String noTransitionMessage(String stateLabel, String event) {
    return "The Negotiation is %s, and no Transition leaves that State on %s."
        .formatted(stateLabel, EVENT_LABELS.get(event));
  }

  private NegotiationGraphV1() {}
}
