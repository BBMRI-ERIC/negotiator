package eu.bbmri_eric.negotiator.characterization.service;

import static eu.bbmri_eric.negotiator.characterization.rest.CanonicalJson.artifact;
import static eu.bbmri_eric.negotiator.characterization.rest.CanonicalJson.namesIn;
import static eu.bbmri_eric.negotiator.characterization.rest.CanonicalJson.publishedValues;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
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

  private static Stream<JsonNode> transitions() {
    return StreamSupport.stream(artifact(DUMP).get("transitions").spliterator(), false);
  }

  private static Set<String> attributesOf(JsonNode securityRule) {
    return namesIn(securityRule.get("attributes"));
  }
}
