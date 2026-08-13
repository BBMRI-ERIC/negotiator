package eu.bbmri_eric.negotiator.characterization.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.negotiator.util.IntegrationTest;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Characterization of {@code GET /v3/resource-lifecycle}, the publicly exposed diagram of the
 * Resource Lifecycle.
 *
 * <p>The endpoint is built by walking the transition set from the initial State and descending into
 * each target as it goes. Because a target can be reached along several paths, the response is
 * <em>path-shaped rather than graph-shaped</em>: it is a nested tree in which whole subtrees repeat,
 * and its size grows with the number of paths through the Lifecycle rather than with the number of
 * transitions. The 13 configured transitions render as 29 nodes.
 *
 * <p>Two consequences are pinned below because a reimplementation from relational configuration has
 * to decide about both: the walk keeps no visited set, so it terminates only because the Resource
 * Lifecycle happens to be acyclic; and States with no outgoing transition never appear as a nesting
 * key, only as a {@code target} value.
 *
 * <p>The full body is compared against a committed fixture. Both are canonicalised first, because
 * the diagram is assembled from {@code HashMap}s and its key order is not guaranteed.
 */
@IntegrationTest(loadTestData = true)
public class ResourceLifecycleDiagramEndpointTest {

  private static final String DIAGRAM = "/v3/resource-lifecycle";

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Autowired private WebApplicationContext context;

  private MockMvc mockMvc;

  @BeforeEach
  void before() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  private String body() throws Exception {
    return mockMvc
        .perform(MockMvcRequestBuilders.get(DIAGRAM))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaTypes.HAL_JSON_VALUE))
        .andReturn()
        .getResponse()
        .getContentAsString();
  }

  private JsonNode tree() throws Exception {
    return MAPPER.readTree(body());
  }

  @Test
  @DisplayName("The whole diagram body, nesting and repeated subtrees included")
  void diagram_isPinnedInFull() throws Exception {
    assertEquals(
        CanonicalJson.fixture("resource-lifecycle-diagram.json"),
        CanonicalJson.canonicalize(body()).strip());
  }

  @Test
  @DisplayName("Reachable without authentication, and carries no HAL links of its own")
  void diagram_isAnonymousAndLinkless() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get(DIAGRAM))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaTypes.HAL_JSON_VALUE))
        .andExpect(jsonPath("$._links").doesNotExist())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$.SUBMITTED").exists());
  }

  @Test
  @DisplayName("Nesting runs 14 objects deep along the longest path")
  void diagram_nestingDepth_isPinned() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get(DIAGRAM))
        .andExpect(
            jsonPath(
                    "$.SUBMITTED.MARK_AS_UNREACHABLE.REPRESENTATIVE_UNREACHABLE.CONTACT"
                        + ".REPRESENTATIVE_CONTACTED.MARK_AS_CHECKING_AVAILABILITY"
                        + ".CHECKING_AVAILABILITY.MARK_AS_AVAILABLE.RESOURCE_AVAILABLE"
                        + ".INDICATE_ACCESS_CONDITIONS.ACCESS_CONDITIONS_INDICATED"
                        + ".ACCEPT_ACCESS_CONDITIONS.ACCESS_CONDITIONS_MET"
                        + ".GRANT_ACCESS_TO_RESOURCE.target")
                .value("RESOURCE_MADE_AVAILABLE"));
  }

  @Test
  @DisplayName("Each transition node carries exactly a target, an event and its target's subtree")
  void diagram_transitionNodeShape_isPinned() throws Exception {
    JsonNode terminal = tree().at("/SUBMITTED/CONTACT/REPRESENTATIVE_CONTACTED/STEP_AWAY");
    assertEquals(2, terminal.size(), "a transition into a State with no outgoing transitions");
    assertEquals("STEP_AWAY", terminal.get("event").asText());
    assertEquals("RESOURCE_UNAVAILABLE", terminal.get("target").asText());

    JsonNode nonTerminal = tree().at("/SUBMITTED/CONTACT");
    assertEquals(3, nonTerminal.size(), "a transition into a State that has outgoing transitions");
    assertEquals("CONTACT", nonTerminal.get("event").asText());
    assertEquals("REPRESENTATIVE_CONTACTED", nonTerminal.get("target").asText());
    assertTrue(
        nonTerminal.has("REPRESENTATIVE_CONTACTED"),
        "the target's own subtree is nested under a key named after the target State");
  }

  @Test
  @DisplayName("Path-shaped, not graph-shaped: whole subtrees repeat verbatim")
  void diagram_repeatsSubtreesReachedAlongDifferentPaths() throws Exception {
    JsonNode root = tree();

    JsonNode viaContact = root.at("/SUBMITTED/CONTACT/REPRESENTATIVE_CONTACTED");
    JsonNode viaUnreachable =
        root.at(
            "/SUBMITTED/MARK_AS_UNREACHABLE/REPRESENTATIVE_UNREACHABLE/CONTACT"
                + "/REPRESENTATIVE_CONTACTED");
    assertFalse(viaContact.isMissingNode());
    assertEquals(viaContact, viaUnreachable, "REPRESENTATIVE_CONTACTED is emitted twice, verbatim");

    // ACCESS_CONDITIONS_INDICATED is reachable along four paths and is therefore emitted four
    // times; the whole diagram is 29 transition nodes for 13 configured transitions.
    assertEquals(4, countKey(root, "ACCESS_CONDITIONS_INDICATED"));
    assertEquals(2, countKey(root, "REPRESENTATIVE_CONTACTED"));
    assertEquals(29, countKey(root, "event"));
  }

  @Test
  @DisplayName("States with no outgoing transition are never nesting keys, only target values")
  void diagram_omitsTerminalAndTransitionlessStates() throws Exception {
    String body = body();
    JsonNode root = MAPPER.readTree(body);
    for (String terminal :
        new String[] {
          "RESOURCE_UNAVAILABLE", "RESOURCE_NOT_MADE_AVAILABLE", "RESOURCE_MADE_AVAILABLE"
        }) {
      assertEquals(0, countKey(root, terminal), terminal + " must not appear as a nesting key");
    }
    // RETURNED_FOR_RESUBMISSION sits on no transition at all, so neither it nor the two Events
    // that are attached to nothing appear anywhere in the diagram - although all three are still
    // published by the metadata endpoints.
    for (String absent :
        new String[] {"RETURNED_FOR_RESUBMISSION", "RETURN_FOR_RESUBMISSION", "OVERRIDE"}) {
      assertFalse(body.contains(absent), absent + " must not appear in the diagram");
    }
  }

  @Test
  @DisplayName("The walk keeps no visited set: it terminates only because the graph is acyclic")
  void diagram_terminationDependsOnTheLifecycleBeingAcyclic() throws Exception {
    JsonNode root = tree();
    Deque<String> statesOnPath = new ArrayDeque<>();
    Iterator<Map.Entry<String, JsonNode>> initial = root.fields();
    while (initial.hasNext()) {
      Map.Entry<String, JsonNode> state = initial.next();
      statesOnPath.push(state.getKey());
      walkTransitions(state.getValue(), statesOnPath);
      statesOnPath.pop();
    }
  }

  /** A transitions map: Event name to transition node. */
  private void walkTransitions(JsonNode transitions, Deque<String> statesOnPath) {
    Iterator<Map.Entry<String, JsonNode>> fields = transitions.fields();
    while (fields.hasNext()) {
      walkTransition(fields.next().getValue(), statesOnPath);
    }
  }

  /**
   * A transition node: {@code target}, {@code event}, and the target's own transitions map nested
   * under a key named after the target State. Nothing in the production walk stops it descending
   * into a target it has already visited, so a State repeating along a root-to-leaf path would mean
   * unbounded recursion rather than a merely larger response.
   */
  private void walkTransition(JsonNode transition, Deque<String> statesOnPath) {
    String target = transition.get("target").asText();
    JsonNode nested = transition.get(target);
    if (nested == null) {
      return;
    }
    assertFalse(
        statesOnPath.contains(target),
        "State " + target + " repeats on the path " + statesOnPath + "; the walk would not stop");
    statesOnPath.push(target);
    walkTransitions(nested, statesOnPath);
    statesOnPath.pop();
  }

  private int countKey(JsonNode node, String key) {
    int count = 0;
    Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
    while (fields.hasNext()) {
      Map.Entry<String, JsonNode> field = fields.next();
      if (field.getKey().equals(key)) {
        count++;
      }
      if (field.getValue().isObject()) {
        count += countKey(field.getValue(), key);
      }
    }
    return count;
  }
}
