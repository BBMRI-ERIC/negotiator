package eu.bbmri_eric.negotiator.characterization.service;

import static eu.bbmri_eric.negotiator.characterization.rest.CanonicalJson.artifact;
import static eu.bbmri_eric.negotiator.characterization.rest.CanonicalJson.namesIn;
import static eu.bbmri_eric.negotiator.characterization.rest.CanonicalJson.publishedValues;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Binds {@link NegotiationGraphV1} to the artifacts it was transcribed from.
 *
 * <p>{@code NegotiationGraphV1} is a hand-written table, and the whole point of the dump slice was
 * that a hand transcription of this graph must not be trusted. Without this class the table would
 * be self-certifying: an assertion phrased over {@code TRANSITIONS} or {@code LEGACY_STATE} alone
 * could only fail if someone edited the constant it reads. Every constant is therefore checked here
 * against a committed artifact that is itself checked against the running system:
 *
 * <ul>
 *   <li>{@code lifecycle/negotiation-graph-v1.json}, the mechanical dump of the Definition graph,
 *       which {@code LifecycleGraphDumpDriftTest} regenerates from the live beans and compares byte
 *       for byte on every run;
 *   <li>{@code characterization/rest/negotiation-events.json}, the published Event metadata, which
 *       {@code LifecycleMetadataEndpointsTest} compares against the live endpoint.
 * </ul>
 *
 * <p>Between them those two artifacts make this a statement about the system rather than about this
 * package, which is what turns the "no Transition targets DRAFT" and "APPROVED is a Legacy State"
 * assertions in {@link NegotiationDraftReachabilityTest} into real findings.
 *
 * <p>No Spring context is needed: both artifacts are files on the test classpath.
 */
class NegotiationGraphV1BindingTest {

  private static final String DUMP = "lifecycle/negotiation-graph-v1.json";
  private static final String PUBLISHED_EVENTS = "characterization/rest/negotiation-events.json";
  private static final String PUBLISHED_STATES = "characterization/rest/negotiation-states.json";

  @Test
  @DisplayName("TRANSITIONS is the dump's transitions array, edge for edge and in the same order")
  void transitions_areTheDumpsTransitions() {
    List<NegotiationGraphV1.Edge> fromDump = new ArrayList<>();
    for (JsonNode transition : artifact(DUMP).get("transitions")) {
      fromDump.add(
          new NegotiationGraphV1.Edge(
              transition.get("source").asText(),
              transition.get("event").asText(),
              transition.get("target").asText()));
    }

    assertEquals(
        fromDump,
        NegotiationGraphV1.TRANSITIONS,
        "the pinned table must be exactly what the mechanical dump says the graph is");
    assertEquals(
        artifact(DUMP).get("transitionCount").asInt(),
        NegotiationGraphV1.TRANSITIONS.size(),
        "the dump counts its own Transitions, and the table must not have gained or lost one");
  }

  @Test
  @DisplayName("INITIAL_STATE is the dump's initialState")
  void initialState_isTheDumpsInitialState() {
    assertEquals(artifact(DUMP).get("initialState").asText(), NegotiationGraphV1.INITIAL_STATE);
  }

  @Test
  @DisplayName("ADMIN_ONLY_EVENTS is exactly the set of ROLE_ADMIN-secured Transitions in the dump")
  void adminOnlyEvents_areTheDumpsSecuredTransitions() {
    Set<String> securedByAdmin =
        transitions()
            .filter(transition -> !transition.get("securityRule").isNull())
            .filter(
                transition ->
                    attributesOf(transition.get("securityRule")).contains(NegotiationGraphV1.ADMIN))
            .map(transition -> transition.get("event").asText())
            .collect(Collectors.toUnmodifiableSet());

    assertEquals(securedByAdmin, NegotiationGraphV1.ADMIN_ONLY_EVENTS);
    assertEquals(
        transitions().filter(transition -> !transition.get("securityRule").isNull()).count(),
        (long) securedByAdmin.size(),
        "a Transition secured by something other than ROLE_ADMIN would not be covered by the"
            + " admin/creator split the authority tests are built on");
  }

  @Test
  @DisplayName("LEGACY_STATE is declared by the dump and named by no Transition of it")
  void legacyState_isDeclaredButUnusedInTheDump() {
    assertTrue(
        namesIn(artifact(DUMP).get("states")).contains(NegotiationGraphV1.LEGACY_STATE),
        "a Legacy State must still be declared, or nothing would resolve the value in old data");
    assertFalse(
        transitions()
            .anyMatch(
                transition ->
                    transition.get("source").asText().equals(NegotiationGraphV1.LEGACY_STATE)
                        || transition
                            .get("target")
                            .asText()
                            .equals(NegotiationGraphV1.LEGACY_STATE)),
        "a Transition naming it would stop it being a Legacy State");
  }

  @Test
  @DisplayName("no Transition of the dump targets DRAFT, and the dump declares DRAFT all the same")
  void draft_isDeclaredByTheDumpButTargetedByNoTransitionOfIt() {
    assertTrue(namesIn(artifact(DUMP).get("states")).contains("DRAFT"));
    assertFalse(
        transitions().anyMatch(transition -> transition.get("target").asText().equals("DRAFT")),
        "DRAFT is not enterable through the graph, which is what makes the seeded Negotiation in"
            + " it an entry from outside the Lifecycle rather than a State it was moved into");
  }

  /**
   * The one place the table deliberately says more than the dump. The dump's {@code events} array
   * lists the Events that actually trigger a Transition - seven of them - while {@code
   * ALL_EVENT_NAMES} is the Event universe the Definition publishes, which is eight. {@code START}
   * is the difference: a real Event, offered by the metadata endpoint, carrying no Transition at
   * all. Keeping it in the table is what lets the refusal coverage cover every name a caller could
   * send, so the two halves are bound to two different artifacts rather than reconciled.
   */
  @Test
  @DisplayName(
      "ALL_EVENT_NAMES is the published Event universe, one wider than the dump's triggers")
  void allEventNames_areThePublishedUniverse_andTheDumpsTriggersAreTheRest() {
    Set<String> published = publishedValues(PUBLISHED_EVENTS, "events");

    assertEquals(
        published,
        NegotiationGraphV1.ALL_EVENT_NAMES,
        "every Event a caller can name is one the metadata endpoint publishes");

    Set<String> triggering = namesIn(artifact(DUMP).get("events"));
    assertEquals(
        published.stream()
            .filter(event -> !event.equals("START"))
            .collect(Collectors.toUnmodifiableSet()),
        triggering,
        "START is the only published Event that triggers no Transition; a second one would need"
            + " the same explanation and does not have it");
    assertEquals(
        triggering,
        transitions()
            .map(transition -> transition.get("event").asText())
            .collect(Collectors.toUnmodifiableSet()),
        "the dump's events array must be the events its transitions array actually uses");
  }

  @Test
  @DisplayName("every refusal message is built from that Event's published label, lowercased")
  void refusalMessages_useThePublishedEventLabels() {
    for (JsonNode event : artifact(PUBLISHED_EVENTS).get("_embedded").get("events")) {
      String name = event.get("value").asText();
      assertEquals(
          "You are not allowed to %s the Negotiation"
              .formatted(event.get("label").asText().toLowerCase()),
          NegotiationGraphV1.refusalMessage(name),
          "the refusal for " + name + " is user-visible text derived from the Event's own label");
    }
  }

  @Test
  @DisplayName("every State the table names is a State the dump and the metadata both declare")
  void statesNamedByTheTable_areDeclaredEverywhere() {
    Set<String> declaredByTheDump = namesIn(artifact(DUMP).get("states"));
    Set<String> published = publishedValues(PUBLISHED_STATES, "states");

    assertEquals(declaredByTheDump, published, "the published States are the Definition's States");

    Set<String> namedByTheTable =
        NegotiationGraphV1.TRANSITIONS.stream()
            .flatMap(edge -> Stream.of(edge.source(), edge.target()))
            .collect(Collectors.toUnmodifiableSet());
    assertTrue(declaredByTheDump.containsAll(namedByTheTable));
    assertTrue(declaredByTheDump.contains(NegotiationGraphV1.INITIAL_STATE));
    assertEquals(
        Set.of(NegotiationGraphV1.LEGACY_STATE),
        declaredByTheDump.stream()
            .filter(state -> !namedByTheTable.contains(state))
            .collect(Collectors.toUnmodifiableSet()),
        "APPROVED is the only declared State no Transition mentions");
    assertEquals(
        declaredByTheDump,
        NegotiationGraphV1.allStateNames(),
        "allStateNames() is derived from the table, and other tests take the Negotiation State"
            + " universe from it, so it must be the dump's States exactly");
  }

  /**
   * The effect each Action bean in the dump has, which is the one place the suite is allowed to
   * know an Action's class name.
   *
   * <p>It belongs here and nowhere else. The dump is a frozen committed artifact, so reading a bean
   * name out of it is a statement about a file rather than about code ADR 0002 rewrites; a
   * behavioural assertion phrased over the same name would instead be guaranteed to go red at
   * cutover, which is the mistake ticket 09 recorded. Everything downstream of this map - {@link
   * NegotiationGraphV1#POST_EFFECTS} and {@link NegotiationPostEffectsTest} - speaks only of
   * effects.
   */
  private static final Map<String, NegotiationGraphV1.PostEffect> EFFECT_OF_ACTION =
      Map.of(
          "EnablePublicPostsAction", NegotiationGraphV1.PostEffect.ENABLE_PUBLIC_POSTS,
          "EnablePrivatePostsAction", NegotiationGraphV1.PostEffect.ENABLE_PRIVATE_POSTS,
          "DisablePostsAction", NegotiationGraphV1.PostEffect.DISABLE_POSTS);

  @Test
  @DisplayName("POST_EFFECTS is the dump's actions arrays, Transition for Transition")
  void postEffects_areTheDumpsActions() {
    Map<NegotiationGraphV1.Edge, Set<NegotiationGraphV1.PostEffect>> fromDump =
        new LinkedHashMap<>();
    for (JsonNode transition : artifact(DUMP).get("transitions")) {
      NegotiationGraphV1.Edge edge =
          new NegotiationGraphV1.Edge(
              transition.get("source").asText(),
              transition.get("event").asText(),
              transition.get("target").asText());
      assertTrue(
          namesIn(transition.get("actions")).size() <= 1,
          ("%s carries more than one Action, so the order its effects are applied in would start"
                  + " to matter and PostFlags.after would have to say which wins")
              .formatted(edge));
      fromDump.put(edge, effectsOf(transition));
    }

    assertEquals(
        fromDump,
        NegotiationGraphV1.POST_EFFECTS,
        "the pinned post effects must be exactly the Actions the mechanical dump attaches, for"
            + " every Transition of the graph and not only the ones that do something");
  }

  /**
   * The asymmetry ticket 06 exists to record, read straight off the dump rather than off the
   * builder chain: a Negotiation abandoned from {@code IN_PROGRESS} has its posts disabled and a
   * Negotiation abandoned from {@code PAUSED} does not. Same Event, same target State, different
   * effect. {@link NegotiationPostEffectsTest} fires both routes and observes it.
   */
  @Test
  @DisplayName("the dump attaches an Action to ABANDON from IN_PROGRESS and none to it from PAUSED")
  void theTwoAbandonRoutes_areNotEquivalentInTheDump() {
    assertEquals(
        Set.of(NegotiationGraphV1.PostEffect.DISABLE_POSTS),
        effectsOf(dumped("IN_PROGRESS", "ABANDON")),
        "abandoning a Negotiation that is running is what disables its posts");
    assertEquals(
        Set.of(),
        effectsOf(dumped("PAUSED", "ABANDON")),
        "abandoning a paused Negotiation reaches the same State by a Transition carrying no Action"
            + " at all, so the posts of the two are left in different places");
    assertEquals(
        NegotiationGraphV1.target("IN_PROGRESS", "ABANDON"),
        NegotiationGraphV1.target("PAUSED", "ABANDON"),
        "the two routes are only worth contrasting because they end in the same State");
  }

  @Test
  @DisplayName("every Action the dump names is one whose effect this suite knows")
  void everyDumpedAction_hasAPinnedEffect() {
    Set<String> dumped =
        transitions()
            .flatMap(transition -> namesIn(transition.get("actions")).stream())
            .collect(Collectors.toUnmodifiableSet());

    assertEquals(
        EFFECT_OF_ACTION.keySet(),
        dumped,
        "a fourth Action, or one fewer, would be a post side effect nothing in this suite pins;"
            + " map it to a PostEffect and add it to POST_EFFECTS rather than widening this test");
  }

  private static Set<NegotiationGraphV1.PostEffect> effectsOf(JsonNode transition) {
    return namesIn(transition.get("actions")).stream()
        .map(
            action -> {
              NegotiationGraphV1.PostEffect effect = EFFECT_OF_ACTION.get(action);
              if (effect == null) {
                throw new IllegalStateException(
                    "The dump attaches an Action this suite has no effect for: " + action);
              }
              return effect;
            })
        .collect(Collectors.toUnmodifiableSet());
  }

  private static JsonNode dumped(String source, String event) {
    return transitions()
        .filter(transition -> transition.get("source").asText().equals(source))
        .filter(transition -> transition.get("event").asText().equals(event))
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "The dump has no Transition '%s' from '%s'".formatted(event, source)));
  }

  private static Stream<JsonNode> transitions() {
    return StreamSupport.stream(artifact(DUMP).get("transitions").spliterator(), false);
  }

  private static Set<String> attributesOf(JsonNode securityRule) {
    return namesIn(securityRule.get("attributes"));
  }
}
