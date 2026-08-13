package eu.bbmri_eric.negotiator.characterization.dump;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.negotiator.util.IntegrationTest;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.statemachine.StateMachine;

/**
 * Drift protection for the frozen v1 Lifecycle graph dump.
 *
 * <p>Regenerates all three artifacts from the live state machine beans and asserts byte equality
 * against the committed copies, so the artifacts can never silently stop describing the beans they
 * claim to describe. Deleted together with the generator at cutover.
 *
 * <p>Also sanity-checks the dump's State and Transition counts against the two configuration
 * classes, so a graph that quietly grows or loses an edge fails here rather than in review.
 */
@IntegrationTest
public class LifecycleGraphDumpDriftTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Autowired
  @Qualifier("negotiationStateMachine")
  private StateMachine<String, String> negotiationStateMachine;

  @Autowired
  @Qualifier("resourceStateMachine")
  private StateMachine<String, String> resourceStateMachine;

  private LifecycleGraphArtifacts regenerated;

  @BeforeEach
  void regenerate() {
    regenerated = LifecycleGraphArtifacts.generate(negotiationStateMachine, resourceStateMachine);
  }

  @Test
  @DisplayName("The committed Negotiation graph dump still matches the live bean")
  void negotiationGraphJsonHasNotDrifted() {
    assertThat(regenerated.content(LifecycleGraphArtifacts.NEGOTIATION_JSON))
        .isEqualTo(committed(LifecycleGraphArtifacts.NEGOTIATION_JSON));
  }

  @Test
  @DisplayName("The committed Resource graph dump still matches the live bean")
  void resourceGraphJsonHasNotDrifted() {
    assertThat(regenerated.content(LifecycleGraphArtifacts.RESOURCE_JSON))
        .isEqualTo(committed(LifecycleGraphArtifacts.RESOURCE_JSON));
  }

  @Test
  @DisplayName("The committed Mermaid diagram still matches the regenerated JSON")
  void mermaidDiagramHasNotDrifted() {
    assertThat(regenerated.content(LifecycleGraphArtifacts.MERMAID))
        .isEqualTo(committed(LifecycleGraphArtifacts.MERMAID));
  }

  @Test
  @DisplayName("Regeneration is byte-reproducible")
  void regenerationIsByteReproducible() {
    LifecycleGraphArtifacts again =
        LifecycleGraphArtifacts.generate(negotiationStateMachine, resourceStateMachine);
    assertThat(again.files()).isEqualTo(regenerated.files());
  }

  @Test
  @DisplayName("Negotiation graph State and Transition counts match NegotiationStateMachineConfig")
  void negotiationGraphCountsMatchTheConfiguration() {
    JsonNode graph = parse(LifecycleGraphArtifacts.NEGOTIATION_JSON);

    // Eight States, because the configuration registers the whole NegotiationState enum rather than
    // only the States its Transitions mention. APPROVED is therefore a Legacy State: declared, but
    // no Transition leads to it.
    assertThat(graph.get("states")).hasSize(8);
    assertThat(graph.get("transitionCount").asInt()).isEqualTo(8);
    assertThat(graph.get("transitions")).hasSize(8);
    assertThat(graph.get("transitions")).allSatisfy(LifecycleGraphDumpDriftTest::isFullyAttached);
  }

  @Test
  @DisplayName("Resource graph State and Transition counts match ResourceStateMachineConfig")
  void resourceGraphCountsMatchTheConfiguration() {
    JsonNode graph = parse(LifecycleGraphArtifacts.RESOURCE_JSON);

    // Twelve States for the same reason; RETURNED_FOR_RESUBMISSION is the Legacy State here.
    assertThat(graph.get("states")).hasSize(12);
    assertThat(graph.get("transitionCount").asInt()).isEqualTo(13);
    assertThat(graph.get("transitions")).hasSize(13);
    assertThat(graph.get("transitions")).allSatisfy(LifecycleGraphDumpDriftTest::isFullyAttached);
  }

  @Test
  @DisplayName("Finding: NegotiationIsApprovedGuard is attached to no Transition of either graph")
  void theOnlyGuardIsAttachedToNothing() {
    assertThat(parse(LifecycleGraphArtifacts.RESOURCE_JSON).get("transitions"))
        .allSatisfy(transition -> assertThat(transition.get("guard").isNull()).isTrue());
    assertThat(parse(LifecycleGraphArtifacts.NEGOTIATION_JSON).get("transitions"))
        .allSatisfy(transition -> assertThat(transition.get("guard").isNull()).isTrue());
  }

  @Test
  @DisplayName("Finding: SUBMITTED is the initial State of both graphs, so DRAFT is no entry State")
  void draftIsDeclaredButIsNotTheInitialState() {
    JsonNode negotiation = parse(LifecycleGraphArtifacts.NEGOTIATION_JSON);

    assertThat(negotiation.get("initialState").asText()).isEqualTo("SUBMITTED");
    assertThat(negotiation.get("states"))
        .anySatisfy(s -> assertThat(s.asText()).isEqualTo("DRAFT"));
    assertThat(negotiation.get("transitions"))
        .anySatisfy(
            transition -> {
              assertThat(transition.get("source").asText()).isEqualTo("DRAFT");
              assertThat(transition.get("event").asText()).isEqualTo("SUBMIT");
              assertThat(transition.get("target").asText()).isEqualTo("SUBMITTED");
            });
    assertThat(negotiation.get("transitions"))
        .noneSatisfy(
            transition -> assertThat(transition.get("target").asText()).isEqualTo("DRAFT"));

    assertThat(parse(LifecycleGraphArtifacts.RESOURCE_JSON).get("initialState").asText())
        .isEqualTo("SUBMITTED");
  }

  @Test
  @DisplayName(
      "Finding: every secured Transition compares ANY, though both configurations declare ALL")
  void securedTransitionsAllCompareAny() {
    assertThat(parse(LifecycleGraphArtifacts.NEGOTIATION_JSON).get("transitions"))
        .filteredOn(transition -> !transition.get("securityRule").isNull())
        .hasSize(2)
        .allSatisfy(LifecycleGraphDumpDriftTest::comparesAnyOverOneAttribute);
    assertThat(parse(LifecycleGraphArtifacts.RESOURCE_JSON).get("transitions"))
        .filteredOn(transition -> !transition.get("securityRule").isNull())
        .hasSize(13)
        .allSatisfy(LifecycleGraphDumpDriftTest::comparesAnyOverOneAttribute);
  }

  private static void comparesAnyOverOneAttribute(JsonNode transition) {
    JsonNode securityRule = transition.get("securityRule");
    assertThat(securityRule.get("comparisonType").asText()).isEqualTo("ANY");
    // A single attribute makes ANY and ALL indistinguishable, which is why the dropped
    // ComparisonType has no observable effect today.
    assertThat(securityRule.get("attributes")).hasSize(1);
  }

  private static void isFullyAttached(JsonNode transition) {
    assertThat(transition.get("source").isNull()).isFalse();
    assertThat(transition.get("event").isNull()).isFalse();
    assertThat(transition.get("target").isNull()).isFalse();
  }

  private JsonNode parse(String fileName) {
    try {
      return MAPPER.readTree(regenerated.content(fileName));
    } catch (Exception e) {
      throw new IllegalStateException("Generated " + fileName + " is not readable JSON", e);
    }
  }

  private static String committed(String fileName) {
    String path = LifecycleGraphArtifacts.RESOURCE_DIRECTORY + "/" + fileName;
    try (InputStream in =
        LifecycleGraphDumpDriftTest.class.getClassLoader().getResourceAsStream(path)) {
      if (in == null) {
        throw new IllegalStateException(
            "Committed artifact " + path + " is missing from the test classpath");
      }
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    } catch (Exception e) {
      throw new IllegalStateException("Could not read committed artifact " + path, e);
    }
  }
}
