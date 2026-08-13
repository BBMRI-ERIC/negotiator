package eu.bbmri_eric.negotiator.characterization.guard;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Keeps the characterization suite able to outlive the subsystem it characterizes.
 *
 * <p>The suite's whole value is that it must pass <em>unchanged</em> after Spring Statemachine is
 * deleted and the four Lifecycle enums with it. That property degrades silently under review
 * pressure, so it is enforced mechanically here: the suite's own source is scanned as text and any
 * forbidden reference fails this test with the file and line that has to change.
 *
 * <p>Two sanctioned exceptions, both deliberate:
 *
 * <ul>
 *   <li>the graph-dump package, which is throwaway by design - it exists precisely to walk Spring
 *       Statemachine's beans and is deleted along with them at cutover;
 *   <li>the adapter implementation, the single place that translates string names into today's
 *       enums.
 * </ul>
 */
class CharacterizationImportGuardTest {

  private static final String CHARACTERIZATION_PACKAGE_PATH =
      "eu/bbmri_eric/negotiator/characterization";

  /**
   * Throwaway generator package, exempt from the Spring Statemachine rule only. It is the one
   * component in this slab whose job is to read the library's beans, and it is deleted at cutover
   * together with the library, so it can never make the parity suite uncompilable.
   */
  private static final String DUMP_PACKAGE = "dump";

  /** The single file allowed to name the four Lifecycle enums. */
  private static final String ADAPTER_IMPLEMENTATION_FILE = "EnumBackedLifecycleTestAdapter.java";

  /**
   * This guard is exempt from its own rules: it cannot state the names it forbids without
   * containing them. Nothing else belongs in this file.
   */
  private static final String GUARD_FILE = "CharacterizationImportGuardTest.java";

  private static final Pattern STATE_MACHINE_LIBRARY =
      Pattern.compile("org\\.springframework\\.statemachine");

  /**
   * Word boundaries matter: {@code NegotiationStateChangeEvent} and {@code
   * ResourceStateChangeEvent} are application events the suite is expected to observe, and they
   * must not be mistaken for the enums {@code NegotiationState} and {@code NegotiationEvent}.
   */
  private static final List<Pattern> LIFECYCLE_ENUMS =
      List.of(
          Pattern.compile("\\bNegotiationState\\b"),
          Pattern.compile("\\bNegotiationEvent\\b"),
          Pattern.compile("\\bNegotiationResourceState\\b"),
          Pattern.compile("\\bNegotiationResourceEvent\\b"));

  @Test
  @DisplayName("no characterization source imports Spring Statemachine")
  void characterizationSuite_doesNotNameTheStateMachineLibrary() {
    List<Violation> violations =
        scan(file -> !isInDumpPackage(file), line -> STATE_MACHINE_LIBRARY.matcher(line).find());

    if (!violations.isEmpty()) {
      fail(
          report(
              violations,
              "The characterization suite must compile without Spring Statemachine",
              """
              Deleting the library must not break the tests that prove the deletion was safe.
              Reach the Lifecycle services through LifecycleTestAdapter instead, and observe only
              what the services return, throw, persist or publish.
              The only exempt package is %s.%s (the throwaway graph-dump generator)."""
                  .formatted(CHARACTERIZATION_PACKAGE_PATH.replace('/', '.'), DUMP_PACKAGE)));
    }
  }

  @Test
  @DisplayName("no characterization source outside the adapter names a Lifecycle enum")
  void characterizationSuite_doesNotNameTheLifecycleEnums() {
    List<Violation> violations =
        scan(
            file -> !file.getFileName().toString().equals(ADAPTER_IMPLEMENTATION_FILE),
            line -> LIFECYCLE_ENUMS.stream().anyMatch(enumName -> enumName.matcher(line).find()));

    if (!violations.isEmpty()) {
      fail(
          report(
              violations,
              "The characterization suite must name States and Events as strings only",
              """
              The four Lifecycle enums are deleted by the redesign, so an assertion naming one
              cannot be re-run against the new subsystem. Write the name as a string literal
              ("SUBMITTED", "APPROVE") and go through LifecycleTestAdapter.
              The only exempt file is %s, which is where the conversion lives."""
                  .formatted(ADAPTER_IMPLEMENTATION_FILE)));
    }
  }

  @Test
  @DisplayName("the guard scans a real, non-empty source tree and its exemptions still exist")
  void guard_scansTheSuiteItClaimsToProtect() {
    List<Path> sources = sourceFiles();
    assertTrue(sources.size() >= 2, "No characterization sources found under " + sourceRoot());

    assertTrue(
        sources.stream()
            .anyMatch(file -> file.getFileName().toString().equals(ADAPTER_IMPLEMENTATION_FILE)),
        """
        The exempted adapter implementation %s no longer exists. If it was renamed, update
        ADAPTER_IMPLEMENTATION_FILE - otherwise this guard exempts a file that is not there and
        the adapter's enum references are no longer covered by any rule."""
            .formatted(ADAPTER_IMPLEMENTATION_FILE));
  }

  private static List<Violation> scan(
      Predicate<Path> fileIsSubjectToRule, Predicate<String> lineBreaksRule) {
    List<Violation> violations = new ArrayList<>();
    for (Path file : sourceFiles()) {
      if (file.getFileName().toString().equals(GUARD_FILE) || !fileIsSubjectToRule.test(file)) {
        continue;
      }
      List<String> code = codeLines(file);
      for (int index = 0; index < code.size(); index++) {
        if (lineBreaksRule.test(code.get(index))) {
          violations.add(new Violation(file, index + 1, code.get(index).strip()));
        }
      }
    }
    return violations;
  }

  private static String report(List<Violation> violations, String headline, String remedy) {
    StringBuilder message = new StringBuilder(headline).append(" (").append(violations.size());
    message.append(violations.size() == 1 ? " violation):\n" : " violations):\n");
    for (Violation violation : violations) {
      message
          .append("  ")
          .append(violation.file().toAbsolutePath())
          .append(':')
          .append(violation.line())
          .append("\n    ")
          .append(violation.text())
          .append('\n');
    }
    return message.append('\n').append(remedy).toString();
  }

  private static boolean isInDumpPackage(Path file) {
    Path relative = sourceRoot().relativize(file.toAbsolutePath());
    return relative.getNameCount() > 1 && relative.getName(0).toString().equals(DUMP_PACKAGE);
  }

  private static List<Path> sourceFiles() {
    try (Stream<Path> tree = Files.walk(sourceRoot())) {
      return tree.filter(Files::isRegularFile)
          .filter(file -> file.getFileName().toString().endsWith(".java"))
          .sorted()
          .toList();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * Resolves the source tree from the working directory rather than the classpath, because the
   * rules are about source text and a compiled class no longer shows an import. Surefire runs with
   * the module directory as working directory; the walk upwards keeps the guard working when tests
   * are launched from the repository root or from an IDE.
   */
  private static Path sourceRoot() {
    Path start = Path.of("").toAbsolutePath();
    for (Path candidate = start; candidate != null; candidate = candidate.getParent()) {
      for (String modulePrefix : List.of("", "backend")) {
        Path root =
            candidate
                .resolve(modulePrefix)
                .resolve("src/test/java")
                .resolve(CHARACTERIZATION_PACKAGE_PATH);
        if (Files.isDirectory(root)) {
          return root;
        }
      }
    }
    throw new IllegalStateException(
        ("Could not locate src/test/java/%s from working directory %s."
                + " The guard must never pass by finding nothing.")
            .formatted(CHARACTERIZATION_PACKAGE_PATH, start));
  }

  /**
   * The file's lines with comments blanked out, so that prose explaining a forbidden name is not
   * itself a violation. Line numbering is preserved.
   */
  private static List<String> codeLines(Path file) {
    List<String> code = new ArrayList<>();
    boolean insideBlockComment = false;
    for (String line : readLines(file)) {
      String remaining = line;
      if (insideBlockComment) {
        int end = remaining.indexOf("*/");
        if (end < 0) {
          code.add("");
          continue;
        }
        remaining = remaining.substring(end + 2);
        insideBlockComment = false;
      }
      int blockStart = remaining.indexOf("/*");
      if (blockStart >= 0) {
        int end = remaining.indexOf("*/", blockStart + 2);
        if (end < 0) {
          remaining = remaining.substring(0, blockStart);
          insideBlockComment = true;
        } else {
          remaining = remaining.substring(0, blockStart) + remaining.substring(end + 2);
        }
      }
      int lineComment = remaining.indexOf("//");
      code.add(lineComment < 0 ? remaining : remaining.substring(0, lineComment));
    }
    return code;
  }

  private static List<String> readLines(Path file) {
    try {
      return Files.readAllLines(file);
    } catch (IOException e) {
      throw new UncheckedIOException("Could not read " + file.toAbsolutePath(), e);
    }
  }

  private record Violation(Path file, int line, String text) {}
}
