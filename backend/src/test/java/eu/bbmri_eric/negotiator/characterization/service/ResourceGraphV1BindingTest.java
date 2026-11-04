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
 * Binds {@link ResourceGraphV1} to the artifacts it was transcribed from.
 *
 * <p>{@code ResourceGraphV1} is hand-written. Without this class, an assertion over {@code
 * TRANSITIONS} or {@code LEGACY_STATE} could fail only if someone edited that constant. So every
 * constant is checked against a committed artifact that is itself checked against the running
 * system:
 *
 * <ul>
 *   <li>{@code lifecycle/resource-graph-v1.json}, the mechanical dump of the Definition graph,
 *       which {@code LifecycleGraphDumpDriftTest} regenerates from the live beans and compares byte
 *       for byte on every run;
 *   <li>{@code characterization/rest/resource-states.json} and {@code
 *       characterization/rest/resource-events.json}, the published metadata, which {@code
 *       LifecycleMetadataEndpointsTest} compares against the live endpoints.
 * </ul>
 *
 * <p>Two facts are pinned here because no service call can observe them: no Transition has a Guard,
 * and every security rule reads back with comparison type {@code ANY}.
 *
 * <p>No Spring context is needed: every artifact is a file on the test classpath.
 */
class ResourceGraphV1BindingTest {

  private static final String DUMP = "lifecycle/resource-graph-v1.json";
  private static final String PUBLISHED_STATES = "characterization/rest/resource-states.json";
  private static final String PUBLISHED_EVENTS = "characterization/rest/resource-events.json";

  @Test
  @DisplayName("TRANSITIONS is the dump's transitions array, edge for edge and in the same order")
  void transitions_areTheDumpsTransitions() {
    List<ResourceGraphV1.Edge> fromDump = new ArrayList<>();
    for (JsonNode transition : artifact(DUMP).get("transitions")) {
      fromDump.add(
          new ResourceGraphV1.Edge(
              transition.get("source").asText(),
              transition.get("event").asText(),
              transition.get("target").asText(),
              soleAttributeOf(transition)));
    }

    assertEquals(
        fromDump,
        ResourceGraphV1.TRANSITIONS,
        "the pinned table must be exactly what the mechanical dump says the graph is");
    assertEquals(
        artifact(DUMP).get("transitionCount").asInt(),
        ResourceGraphV1.TRANSITIONS.size(),
        "the dump counts its own Transitions, and the table must not have gained or lost one");
  }

  @Test
  @DisplayName("INITIAL_STATE is the dump's initialState")
  void initialState_isTheDumpsInitialState() {
    assertEquals(artifact(DUMP).get("initialState").asText(), ResourceGraphV1.INITIAL_STATE);
  }

  /**
   * {@code allStateNames()} is derived from the Transition table plus the Legacy State, so this
   * checks its twelve States are the twelve the dump declares and the metadata publishes.
   */
  @Test
  @DisplayName("allStateNames() is the dump's States and the States the metadata publishes")
  void allStateNames_areTheDumpsStatesAndThePublishedStates() {
    assertEquals(
        namesIn(artifact(DUMP).get("states")),
        ResourceGraphV1.allStateNames(),
        "the Definition declares the whole enum, so the derived set has to carry all twelve");
    assertEquals(
        publishedValues(PUBLISHED_STATES, "states"),
        ResourceGraphV1.allStateNames(),
        "the published States are the Definition's States");
    assertTrue(
        ResourceGraphV1.allStateNames().contains(ResourceGraphV1.INITIAL_STATE),
        "the initial State must be one of the declared States");
  }

  /**
   * The dump's {@code events} array lists the eleven Events that trigger a Transition; {@code
   * ALL_EVENT_NAMES} holds the thirteen the Definition publishes. {@code RETURN_FOR_RESUBMISSION}
   * and {@code OVERRIDE} are the difference: published, with no Transition.
   */
  @Test
  @DisplayName("ALL_EVENT_NAMES is the published universe, two wider than the dump's triggers")
  void allEventNames_areThePublishedUniverse_andTheDumpsTriggersAreTheRest() {
    Set<String> published = publishedValues(PUBLISHED_EVENTS, "events");

    assertEquals(
        published,
        ResourceGraphV1.ALL_EVENT_NAMES,
        "every Event a caller can name is one the metadata endpoint publishes");

    Set<String> triggering = namesIn(artifact(DUMP).get("events"));
    assertEquals(
        published.stream()
            .filter(event -> !ResourceGraphV1.EVENTS_ON_NO_TRANSITION.contains(event))
            .collect(Collectors.toUnmodifiableSet()),
        triggering,
        "a third published Event carrying no Transition would need the same explanation these two"
            + " have and does not have it");
    assertEquals(
        triggering,
        transitions()
            .map(transition -> transition.get("event").asText())
            .collect(Collectors.toUnmodifiableSet()),
        "the dump's events array must be the events its transitions array actually uses");
  }

  @Test
  @DisplayName("every Transition is secured, by exactly one of the three known rule names")
  void everyTransition_carriesExactlyOneOfTheThreeKnownRules() {
    transitions()
        .forEach(
            transition -> {
              assertFalse(
                  transition.get("securityRule").isNull(),
                  "an unsecured Transition would be offered to everybody, and the authority tests"
                      + " assume every Transition belongs to exactly one kind of caller");
              assertEquals(
                  1,
                  transition.get("securityRule").get("attributes").size(),
                  "a rule with two attributes would make ComparisonType observable; see"
                      + " everySecurityRule_comparesAny");
              assertTrue(
                  ResourceGraphV1.ALL_AUTHORITY_RULES.contains(soleAttributeOf(transition)),
                  "the service can only evaluate isCreator, isRepresentative and isAdmin; any other"
                      + " attribute silently falls through to 'rule met'");
            });
  }

  @Test
  @DisplayName("the per-rule secured Event sets are the dump's security rules")
  void securedEvents_perRule_areTheDumpsSecurityRules() {
    for (String rule : ResourceGraphV1.ALL_AUTHORITY_RULES) {
      assertEquals(
          transitions()
              .filter(transition -> soleAttributeOf(transition).equals(rule))
              .map(transition -> transition.get("event").asText())
              .collect(Collectors.toUnmodifiableSet()),
          ResourceGraphV1.eventsSecuredBy(rule),
          "the Events reserved for " + rule + " must be the ones the dump reserves for it");
    }

    assertEquals(
        ResourceGraphV1.TRANSITIONS.size(),
        ResourceGraphV1.ALL_AUTHORITY_RULES.stream()
            .mapToLong(
                rule ->
                    ResourceGraphV1.TRANSITIONS.stream()
                        .filter(edge -> edge.requiredAuthority().equals(rule))
                        .count())
            .sum(),
        "every Transition of the table must be accounted for by one of the three rules");
  }

  /**
   * {@code NegotiationIsApprovedGuard} is attached to nothing.
   *
   * <p>{@code ResourceStateMachineConfig} ends with a {@code
   * transitions.withExternal().guard(negotiationIsApproved())} fragment that names no source, event
   * or target, and Spring Statemachine drops it without a word. The dump records the thirteen fully
   * specified Transitions, each with {@code "guard": null}. {@code LifecycleGraphDumperUnwrapTest}
   * shows the dumper would name the Guard if a Transition had it.
   *
   * <p>The check in {@code ResourceLifecycleServiceImpl} is what enforces "the parent Negotiation
   * must be IN_PROGRESS", pinned by {@link
   * ResourcePossibleEventsAuthorityTest#possibleEvents_areGatedOnTheParentBeingInProgress}. A Guard
   * that has never fired should not be carried into a new implementation.
   */
  @Test
  @DisplayName("no Transition of the graph carries a Guard, and there is no fourteenth orphan one")
  void noTransition_carriesAGuard() {
    assertEquals(
        ResourceGraphV1.TRANSITIONS.size(),
        artifact(DUMP).get("transitionCount").asInt(),
        "the guard(...) fragment produces no Transition of its own; a fourteenth entry would mean"
            + " NegotiationIsApprovedGuard is live after all");
    transitions()
        .forEach(
            transition ->
                assertTrue(
                    transition.get("guard").isNull(),
                    "the graph has exactly one Guard bean and the dump shows it attached to"
                        + " nothing"));
  }

  /**
   * {@code ResourceStateMachineConfig} passes {@code ComparisonType.ALL} to all thirteen {@code
   * secured(...)} calls and the dump reads {@code ANY} back from all thirteen, because Spring
   * Statemachine 4.0.0's {@code AbstractTransitionConfigurer.setSecurityRule} ignores the argument.
   * Pinned as observed, because that is the behaviour a reimplementation has to reproduce.
   */
  @Test
  @DisplayName("every security rule reads back ANY, whatever the configuration asked for")
  void everySecurityRule_comparesAny() {
    transitions()
        .forEach(
            transition ->
                assertEquals(
                    ResourceGraphV1.COMPARISON_TYPE,
                    transition.get("securityRule").get("comparisonType").asText()));
  }

  @Test
  @DisplayName("LEGACY_STATE is declared by the dump and named by no Transition of it")
  void legacyState_isDeclaredButUnusedInTheDump() {
    assertTrue(
        namesIn(artifact(DUMP).get("states")).contains(ResourceGraphV1.LEGACY_STATE),
        "a Legacy State must still be declared, or nothing would resolve the value in old data");
    assertFalse(
        transitions()
            .anyMatch(
                transition ->
                    transition.get("source").asText().equals(ResourceGraphV1.LEGACY_STATE)
                        || transition.get("target").asText().equals(ResourceGraphV1.LEGACY_STATE)),
        "a Transition naming it would stop it being a Legacy State");
    assertEquals(
        Set.of(ResourceGraphV1.LEGACY_STATE),
        namesIn(artifact(DUMP).get("states")).stream()
            .filter(state -> !statesNamedByTransitions().contains(state))
            .collect(Collectors.toUnmodifiableSet()),
        "RETURNED_FOR_RESUBMISSION is the only declared State no Transition mentions, checked"
            + " against the dump's States because the table derives its States from Transitions");
  }

  /**
   * The parent State the gate demands belongs to the Negotiation graph, so it is checked against
   * that graph's table. {@link ResourcePossibleEventsAuthorityTest} walks every Negotiation State
   * through the gate, taking them from the same table.
   */
  @Test
  @DisplayName("REQUIRED_PARENT_STATE is one of the Negotiation graph's declared States")
  void requiredParentState_isANegotiationState() {
    assertTrue(
        NegotiationGraphV1.allStateNames().contains(ResourceGraphV1.REQUIRED_PARENT_STATE),
        "the gate compares against a Negotiation State, and it must be a State that exists");
  }

  private static Set<String> statesNamedByTransitions() {
    return ResourceGraphV1.TRANSITIONS.stream()
        .flatMap(edge -> Stream.of(edge.source(), edge.target()))
        .collect(Collectors.toUnmodifiableSet());
  }

  private static String soleAttributeOf(JsonNode transition) {
    return transition.get("securityRule").get("attributes").get(0).asText();
  }

  private static Stream<JsonNode> transitions() {
    return StreamSupport.stream(artifact(DUMP).get("transitions").spliterator(), false);
  }
}
