package eu.bbmri_eric.negotiator.characterization.dump;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import eu.bbmri_eric.negotiator.util.IntegrationTest;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.statemachine.StateMachine;

/**
 * Regenerates the frozen v1 Lifecycle graph artifacts from the live state machine beans.
 *
 * <p>This is the generator, not the gate. It only runs when explicitly asked:
 *
 * <pre>
 *   scripts/test-backend.sh -f backend \
 *     'eu.bbmri_eric.negotiator.characterization.dump.LifecycleGraphDumpGeneratorTest' \
 *     -Dlifecycle.dump.regenerate=true
 * </pre>
 *
 * <p>{@code LifecycleGraphDumpDriftTest} is what runs on every build and fails if the committed
 * artifacts no longer match the beans.
 */
@IntegrationTest
@EnabledIfSystemProperty(
    named = "lifecycle.dump.regenerate",
    matches = "true",
    disabledReason = "Generator - run with -Dlifecycle.dump.regenerate=true to rewrite the dump")
public class LifecycleGraphDumpGeneratorTest {

  @Autowired
  @Qualifier("negotiationStateMachine")
  private StateMachine<String, String> negotiationStateMachine;

  @Autowired
  @Qualifier("resourceStateMachine")
  private StateMachine<String, String> resourceStateMachine;

  @Test
  @DisplayName("Regenerates the frozen v1 Lifecycle graph dump from the live beans")
  void regeneratesFrozenArtifacts() throws Exception {
    assertNotNull(negotiationStateMachine);
    assertNotNull(resourceStateMachine);

    LifecycleGraphArtifacts artifacts =
        LifecycleGraphArtifacts.generate(negotiationStateMachine, resourceStateMachine);

    Path target = artifactSourceDirectory();
    Files.createDirectories(target);
    for (Map.Entry<String, String> file : artifacts.files().entrySet()) {
      Files.writeString(target.resolve(file.getKey()), file.getValue(), StandardCharsets.UTF_8);
      System.out.println("Wrote " + target.resolve(file.getKey()).toAbsolutePath());
    }
  }

  /**
   * Locates {@code src/test/resources/lifecycle} whether the build runs from the repository root or
   * from the backend module directory.
   */
  private static Path artifactSourceDirectory() {
    Path fromModule = Path.of("src", "test", "resources");
    Path base =
        Files.isDirectory(fromModule) ? fromModule : Path.of("backend", "src", "test", "resources");
    return base.resolve(LifecycleGraphArtifacts.RESOURCE_DIRECTORY);
  }
}
