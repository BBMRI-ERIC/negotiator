package eu.bbmri_eric.negotiator.characterization.dump;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.statemachine.StateMachine;

/**
 * The frozen v1 dump of both Lifecycle Definition graphs, as filename to file content.
 *
 * <p>Produced whole or not at all: {@link LifecycleGraphDumper} throws if any Guard or Action
 * cannot be reflectively unwrapped, so a caller that receives an instance of this class holds a
 * complete, faithful set. Nothing here writes to disk - see {@code
 * LifecycleGraphDumpGeneratorTest}.
 */
final class LifecycleGraphArtifacts {

  static final String RESOURCE_DIRECTORY = "lifecycle";
  static final String NEGOTIATION_JSON = "negotiation-graph-v1.json";
  static final String RESOURCE_JSON = "resource-graph-v1.json";
  static final String MERMAID = "graphs-v1.mmd";

  private final Map<String, String> files;

  private LifecycleGraphArtifacts(Map<String, String> files) {
    this.files = files;
  }

  /** Walks both live beans and renders all three artifacts. */
  static LifecycleGraphArtifacts generate(
      StateMachine<String, String> negotiationStateMachine,
      StateMachine<String, String> resourceStateMachine) {
    String negotiationJson =
        LifecycleGraphDumper.toCanonicalJson(
            "negotiation", "negotiationStateMachine", negotiationStateMachine);
    String resourceJson =
        LifecycleGraphDumper.toCanonicalJson(
            "resource", "resourceStateMachine", resourceStateMachine);

    Map<String, String> files = new LinkedHashMap<>();
    files.put(NEGOTIATION_JSON, negotiationJson);
    files.put(RESOURCE_JSON, resourceJson);
    files.put(MERMAID, LifecycleGraphDumper.toMermaid(negotiationJson, resourceJson));
    return new LifecycleGraphArtifacts(files);
  }

  Map<String, String> files() {
    return Map.copyOf(files);
  }

  String content(String fileName) {
    String content = files.get(fileName);
    if (content == null) {
      throw new IllegalArgumentException("No generated artifact named " + fileName);
    }
    return content;
  }
}
