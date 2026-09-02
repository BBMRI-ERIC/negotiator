package eu.bbmri_eric.negotiator.lifecycle.evaluation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * ADR 0001's "the evaluator is stateless and does no I/O of its own" as four executable rules
 * rather than as a sentence in a javadoc.
 *
 * <p>The ADR is explicit that this is "a deliberate constraint rather than a performance claim: an
 * evaluator that structurally cannot query the database makes loading the definition graph an
 * explicit, testable step", and that an engine owning its own persistence "would invite an N+1
 * across a negotiation's resources, discovered only under load". A constraint whose only
 * enforcement is that nobody has broken it yet is a convention. These rules are what make it
 * structural.
 *
 * <p>Built in the register of {@code DefinitionInertnessGuardTest}: the Java source is scanned as
 * <em>text</em>, comments are blanked so prose naming a forbidden term is not a violation, every
 * violation is reported with a {@code file:line} a reader can check, and a meta-test stops the
 * whole thing passing by scanning nothing.
 *
 * <p>Unlike that guard, this one is <b>not</b> meant to be deleted. The inertness guard exists to
 * prove a temporary state and dies when a slab starts reading the definition tables; this one
 * states a permanent property of the subsystem, and the slab that legitimately needs to load a
 * graph does so in the definition package, on the other side of {@link
 * eu.bbmri_eric.negotiator.lifecycle.graph.GuardCatalogue} — not by relaxing a rule here.
 */
class EvaluatorPurityGuardTest {

  private static final String GRAPH_PACKAGE = "eu/bbmri_eric/negotiator/lifecycle/graph";
  private static final String EVALUATION_PACKAGE = "eu/bbmri_eric/negotiator/lifecycle/evaluation";

  /** Below this, the scan has lost its way and would pass by finding nothing. */
  private static final int MINIMUM_SCANNED_SOURCES = 15;

  /**
   * Packages that only exist to reach a database. {@code org.springframework.stereotype} is
   * deliberately absent — being a Spring bean is not the problem, holding a query is.
   */
  private static final List<String> PERSISTENCE_PACKAGES =
      List.of("jakarta.persistence", "org.springframework.data", "org.hibernate", "java.sql");

  /** {@code EntityManager} is named outright because ADR 0001 names it outright. */
  private static final Pattern PERSISTENCE_TYPE =
      Pattern.compile("\\b(?:EntityManager|EntityManagerFactory|JdbcTemplate|DataSource)\\b");

  /**
   * Any repository at all, by the convention every one in this backend follows. Deliberately a
   * suffix rather than a list of names: the point is that no repository <em>that does not exist
   * yet</em> can be injected here either.
   */
  private static final Pattern REPOSITORY_TYPE = Pattern.compile("\\b\\w*Repository\\b");

  @Test
  void neitherPackage_namesAnythingThatCouldReachADatabase() {
    List<Violation> violations =
        scan(
            anySource(),
            line ->
                PERSISTENCE_PACKAGES.stream().anyMatch(line::contains)
                    || PERSISTENCE_TYPE.matcher(line).find()
                    || REPOSITORY_TYPE.matcher(line).find());

    assertTrue(
        violations.isEmpty(),
        report(
            violations,
            """
            The Transition Evaluator must be unable to query the database, structurally.

            Loading a definition graph is the definition package's work, behind GuardCatalogue and
            ActionCatalogue. Whatever wants data here wants a port on the constructor instead - see
            InformationRequirementSatisfaction, which is the one question about a move that could
            not be handed in on the evaluation context."""));
  }

  /**
   * The dependency direction, in the direction that matters. {@code definition} compiles into
   * {@code graph}; neither {@code graph} nor {@code evaluation} may reach back, or the inertness
   * the definition package is holding would be gone and the compiler's package-privacy pointless.
   */
  @Test
  void neitherPackage_reachesIntoTheDefinitionPackage() {
    List<Violation> violations =
        scan(anySource(), line -> line.contains("eu.bbmri_eric.negotiator.lifecycle.definition"));

    assertTrue(
        violations.isEmpty(),
        report(
            violations,
            """
            The definition package depends on the graph package, never the other way round.

            They meet at GuardCatalogue and ActionCatalogue, which the evaluation package implements
            and the definition package calls. An import in this direction is a cycle, and it would
            also put the definition entities within the evaluator's reach - which is the whole thing
            DefinitionInertnessGuardTest is currently proving has not happened."""));
  }

  /**
   * And the direction inside the subsystem. The graph is the vocabulary both other packages agree
   * on, so it must not know that a pipeline or a registry exists — otherwise a compiled graph could
   * not be built by a test, or by a seed, without dragging the evaluator in.
   */
  @Test
  void theGraphPackage_doesNotKnowTheEvaluationPackageExists() {
    List<Violation> violations =
        scan(
            path -> path.toString().contains(GRAPH_PACKAGE),
            line -> line.contains("eu.bbmri_eric.negotiator.lifecycle.evaluation"));

    assertTrue(
        violations.isEmpty(),
        report(
            violations,
            """
            The graph package is the vocabulary the other two agree on, and holds no machinery.

            A compiled graph must be constructible from names and flags alone - by a compile, by a
            test, or one day by whatever reads a definition file. An import of the evaluation package
            makes all three depend on the pipeline."""));
  }

  /**
   * Anti-vacuity, in both halves. The scan must actually be reading files, and each rule must
   * actually fire on the thing it forbids — a guard built out of a regex that matches nothing
   * passes for ever and proves nothing.
   */
  @Test
  void theRules_scanTheCodeTheyClaimToAndMatchWhatTheyForbid() {
    List<Path> scanned = sources(anySource());
    assertTrue(
        scanned.size() >= MINIMUM_SCANNED_SOURCES,
        "the purity scan found only %d sources under the graph and evaluation packages, expected at"
            + " least %d - it is not scanning what it claims to"
                .formatted(scanned.size(), MINIMUM_SCANNED_SOURCES));

    assertTrue(REPOSITORY_TYPE.matcher("private final StateRepository states;").find());
    assertTrue(REPOSITORY_TYPE.matcher("SomeFutureRepository r;").find());
    assertTrue(PERSISTENCE_TYPE.matcher("@PersistenceContext EntityManager em;").find());
    assertTrue(
        "import org.springframework.data.jpa.repository.Query;"
            .contains("org.springframework.data"));

    assertFalse(REPOSITORY_TYPE.matcher("GuardCatalogue catalogue;").find());
    assertFalse(PERSISTENCE_TYPE.matcher("EvaluationContext context;").find());
    assertFalse(
        "import org.springframework.stereotype.Component;".contains("org.springframework.data"),
        "being a Spring bean is not what this guard forbids");
  }

  private static Predicate<Path> anySource() {
    return path ->
        path.toString().contains(GRAPH_PACKAGE) || path.toString().contains(EVALUATION_PACKAGE);
  }

  private static List<Violation> scan(Predicate<Path> files, Predicate<String> offending) {
    List<Violation> violations = new ArrayList<>();
    for (Path file : sources(files)) {
      List<String> lines = codeLines(file);
      for (int i = 0; i < lines.size(); i++) {
        if (offending.test(lines.get(i))) {
          violations.add(new Violation(file, i + 1, lines.get(i).strip()));
        }
      }
    }
    return violations;
  }

  private static List<Path> sources(Predicate<Path> wanted) {
    Path root = moduleRoot().resolve("src/main/java");
    try (Stream<Path> walk = Files.walk(root)) {
      return walk.filter(Files::isRegularFile)
          .filter(path -> path.toString().endsWith(".java"))
          .filter(wanted)
          .sorted()
          .toList();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /** Blanks comments while preserving line numbers, so prose naming a term is not a violation. */
  private static List<String> codeLines(Path file) {
    List<String> lines;
    try {
      lines = Files.readAllLines(file);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    List<String> code = new ArrayList<>(lines.size());
    boolean inBlockComment = false;
    for (String line : lines) {
      String working = line;
      if (inBlockComment) {
        int end = working.indexOf("*/");
        if (end < 0) {
          code.add("");
          continue;
        }
        working = " ".repeat(end + 2) + working.substring(end + 2);
        inBlockComment = false;
      }
      int blockStart = working.indexOf("/*");
      if (blockStart >= 0) {
        working = working.substring(0, blockStart);
        inBlockComment = !line.substring(blockStart).contains("*/");
      }
      int lineStart = working.indexOf("//");
      if (lineStart >= 0) {
        working = working.substring(0, lineStart);
      }
      code.add(working);
    }
    return code;
  }

  /**
   * Walks upward looking for a directory that holds both packages, trying {@code backend} as a
   * prefix at each level, and throws rather than passing by finding nothing.
   */
  private static Path moduleRoot() {
    Path candidate = Path.of("").toAbsolutePath();
    while (candidate != null) {
      for (String prefix : List.of("", "backend")) {
        Path module = prefix.isEmpty() ? candidate : candidate.resolve(prefix);
        if (Files.isDirectory(module.resolve("src/main/java").resolve(GRAPH_PACKAGE))
            && Files.isDirectory(module.resolve("src/main/java").resolve(EVALUATION_PACKAGE))) {
          return module;
        }
      }
      candidate = candidate.getParent();
    }
    throw new IllegalStateException(
        "could not find the module holding the lifecycle graph and evaluation packages from "
            + Path.of("").toAbsolutePath());
  }

  private static String report(List<Violation> violations, String explanation) {
    StringBuilder message = new StringBuilder();
    message
        .append("The Lifecycle evaluator must stay pure (")
        .append(violations.size())
        .append(violations.size() == 1 ? " violation" : " violations")
        .append("):\n");
    for (Violation violation : violations) {
      message
          .append("  ")
          .append(violation.file())
          .append(':')
          .append(violation.line())
          .append("\n    ")
          .append(violation.text())
          .append('\n');
    }
    return message.append('\n').append(explanation).toString();
  }

  private record Violation(Path file, int line, String text) {}
}
