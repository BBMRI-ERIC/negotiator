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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Checks that the Transition Evaluator holds no state and does no I/O of its own, with four rules
 * and two tests that prove the rules are not vacuous.
 *
 * <p>An evaluator that cannot query the database forces loading the Compiled Graph into a separate,
 * testable step. An evaluator that owned its persistence would invite an N+1 query across a
 * Negotiation's Resources that shows up only under load.
 *
 * <p>Three rules are blocklists and the fourth is an allowlist, see {@link
 * #IMPORTS_PERMITTED_IN_GRAPH}. A blocklist stops only what someone thought of, and the graph
 * package is small enough to have a closed list of dependencies.
 *
 * <p>The scan reads Java source as text and blanks comments, so a comment naming a forbidden term
 * is not a violation. Each violation carries a {@code file:line}. Each rule is shown to fire on
 * what it forbids and to spare something harmless, and a meta-test stops the guard passing by
 * scanning nothing.
 *
 * <p>This guard is permanent. Code that needs to load a graph belongs in the definition package,
 * behind {@link eu.bbmri_eric.negotiator.lifecycle.graph.GuardCatalogue}, not in a relaxed rule
 * here.
 */
class EvaluatorPurityGuardTest {

  private static final String GRAPH_PACKAGE = "eu.bbmri_eric.negotiator.lifecycle.graph";
  private static final String EVALUATION_PACKAGE = "eu.bbmri_eric.negotiator.lifecycle.evaluation";
  private static final String DEFINITION_PACKAGE = "eu.bbmri_eric.negotiator.lifecycle.definition";

  private static final String GRAPH_PACKAGE_PATH = GRAPH_PACKAGE.replace('.', '/');
  private static final String EVALUATION_PACKAGE_PATH = EVALUATION_PACKAGE.replace('.', '/');

  /** Below this, the scan has lost its way and would pass by finding nothing. */
  private static final int MINIMUM_SCANNED_SOURCES = 15;

  /**
   * A separate floor for the graph package, because two of the four rules scan it alone. If the
   * walk found no graph files, the evaluation package's files would still clear {@link
   * #MINIMUM_SCANNED_SOURCES} and the allowlist would check nothing.
   */
  private static final int MINIMUM_SCANNED_GRAPH_SOURCES = 8;

  /**
   * Packages that exist only to reach a database. {@code org.springframework.stereotype} is absent
   * because being a Spring bean is allowed. Holding a query is not.
   */
  private static final List<String> PERSISTENCE_PACKAGES =
      List.of("jakarta.persistence", "org.springframework.data", "org.hibernate", "java.sql");

  private static final Pattern PERSISTENCE_TYPE =
      Pattern.compile("\\b(?:EntityManager|EntityManagerFactory|JdbcTemplate|DataSource)\\b");

  /**
   * Any repository, by this backend's naming convention. It matches a suffix instead of a list of
   * names, so a repository added later is caught too.
   */
  private static final Pattern REPOSITORY_TYPE = Pattern.compile("\\b\\w*Repository\\b");

  /**
   * A single, static or wildcard import, with the imported name captured.
   *
   * <p>The character class needs {@code *}. Without it {@code import
   * com.fasterxml.jackson.databind.*;} matches nothing and passes the allowlist. Google Java Style
   * forbids wildcard imports, but the formatter does not enforce it and production code has some.
   */
  private static final Pattern IMPORT_STATEMENT =
      Pattern.compile("^\\s*import\\s+(?:static\\s+)?([\\w.$*]+)\\s*;");

  /**
   * Everything the graph package may depend on. The blocklists cannot stop {@code graph} importing
   * Jackson, a Spring annotation or something under {@code common/}. The graph package holds only
   * the types the other two packages share, so it can live with a closed list where the evaluation
   * package cannot.
   *
   * <ul>
   *   <li>{@code java.}: a Compiled Graph is names, flags and collections, and a test or a compile
   *       must be able to build one without a container.
   *   <li>{@code lombok.}: its annotations have {@code SOURCE} retention at {@code provided} scope,
   *       so nothing from Lombok reaches the runtime classpath. The exception is {@code
   *       lombok.extern}, which generates code that names a runtime logger. Graph types do not log,
   *       but no rule here would stop it.
   * </ul>
   *
   * <p>Add a third entry only if the import cannot reach a database, a container or a serializer,
   * and a test can still build a Compiled Graph without anything else. {@code
   * org.springframework.lang.Nullable} fails this, because it is a runtime dependency, so optional
   * components are documented in javadoc instead.
   */
  private static final List<String> IMPORTS_PERMITTED_IN_GRAPH = List.of("java.", "lombok.");

  @Test
  @DisplayName("neither package names anything that could reach a database")
  void neitherPackage_namesAnythingThatCouldReachADatabase() {
    List<Violation> violations = scan(anySource(), EvaluatorPurityGuardTest::couldReachADatabase);

    assertTrue(
        violations.isEmpty(),
        report(
            violations,
            """
            The Transition Evaluator must be unable to query the database, structurally.

            Loading a definition graph is the definition package's work, behind GuardCatalogue and
            ActionCatalogue. Code here that needs data takes a port on the constructor instead, as
            InformationRequirementSatisfaction does."""));
  }

  /**
   * {@code definition} compiles into {@code graph}. Neither {@code graph} nor {@code evaluation}
   * may import {@code definition}, or the definition entities' package privacy stops protecting
   * them.
   */
  @Test
  @DisplayName("neither package reaches into the definition package")
  void neitherPackage_reachesIntoTheDefinitionPackage() {
    List<Violation> violations =
        scan(anySource(), EvaluatorPurityGuardTest::namesTheDefinitionPackage);

    assertTrue(
        violations.isEmpty(),
        report(
            violations,
            """
            The definition package depends on the graph package, never the other way round.

            They meet at GuardCatalogue and ActionCatalogue, which the evaluation package implements
            and the definition package calls. An import in this direction is a cycle, and it would
            also put the definition entities within the evaluator's reach.

            The definition package reads its tables through LifecycleDefinitions, which returns a
            CompiledGraph. Code here that needs a graph should receive one. Code that needs rows
            belongs in another package."""));
  }

  /**
   * The graph package must not know the pipeline or a registry exists, or a test or a seed could
   * not build a Compiled Graph without the evaluator.
   */
  @Test
  @DisplayName("the graph package does not know the evaluation package exists")
  void theGraphPackage_doesNotKnowTheEvaluationPackageExists() {
    List<Violation> violations =
        scan(graphSource(), EvaluatorPurityGuardTest::namesTheEvaluationPackage);

    assertTrue(
        violations.isEmpty(),
        report(
            violations,
            """
            The graph package is the vocabulary the other two agree on, and holds no machinery.

            A Compiled Graph must be buildable from names and flags alone, by a compile or a test.
            An import of the evaluation package makes both depend on the pipeline."""));
  }

  /**
   * Lists what is permitted instead of what is forbidden, so it also stops what nobody thought of.
   * It overlaps the rules above on purpose.
   *
   * @see #IMPORTS_PERMITTED_IN_GRAPH for why the list has exactly two entries
   */
  @Test
  @DisplayName("the graph package imports nothing but the JDK and Lombok")
  void theGraphPackage_importsNothingButTheJdkAndLombok() {
    List<Violation> violations =
        scan(graphSource(), EvaluatorPurityGuardTest::isAnImportTheGraphMayNotHave);

    assertTrue(
        violations.isEmpty(),
        report(
            violations,
            """
            The graph package's dependencies are an allowlist: java. and lombok., and nothing else.

            This list is everything the graph package may depend on, so a dependency nobody thought
            to forbid is still forbidden. Only this rule catches Jackson, a Spring annotation or
            anything under common/.

            To widen it, write down the reason next to the two existing entries, in the same form
            as theirs."""));
  }

  /**
   * Every rule fires on what it forbids and spares something harmless. A regex that matches nothing
   * always passes, and one that matches everything gets switched off.
   */
  @Test
  @DisplayName("each rule matches what it forbids, and spares what it must allow")
  void theRules_matchWhatTheyForbidAndSpareWhatTheyMustAllow() {
    assertTrue(couldReachADatabase("private final StateRepository states;"));
    assertTrue(couldReachADatabase("SomeFutureRepository r;"));
    assertTrue(couldReachADatabase("@PersistenceContext EntityManager em;"));
    assertTrue(couldReachADatabase("import org.springframework.data.jpa.repository.Query;"));
    assertTrue(couldReachADatabase("import jakarta.persistence.Entity;"));
    assertTrue(couldReachADatabase("import org.hibernate.annotations.Immutable;"));
    assertTrue(couldReachADatabase("import java.sql.Connection;"));

    assertFalse(couldReachADatabase("GuardCatalogue catalogue;"));
    assertFalse(couldReachADatabase("EvaluationContext context;"));
    assertFalse(
        couldReachADatabase("import org.springframework.stereotype.Component;"),
        "being a Spring bean is not what this guard forbids");

    assertTrue(
        namesTheDefinitionPackage("import %s.LifecycleDefinition;".formatted(DEFINITION_PACKAGE)));
    assertTrue(
        namesTheDefinitionPackage("%s.DefinitionCompiler compiler;".formatted(DEFINITION_PACKAGE)),
        "a fully-qualified name is the other way this edge can be written");
    assertFalse(
        namesTheDefinitionPackage("import %s.CompiledGraph;".formatted(GRAPH_PACKAGE)),
        "the lifecycle prefix is not what this rule forbids, one package under it is");

    assertTrue(namesTheEvaluationPackage("import %s.GuardRegistry;".formatted(EVALUATION_PACKAGE)));
    assertFalse(
        namesTheEvaluationPackage("import %s.GuardCatalogue;".formatted(GRAPH_PACKAGE)),
        "the catalogue is where these two packages are supposed to meet");

    assertTrue(
        isAnImportTheGraphMayNotHave("import com.fasterxml.jackson.databind.JsonNode;"),
        "a serializer is what the allowlist exists for, and no blocklist mentions it");
    assertTrue(isAnImportTheGraphMayNotHave("import org.springframework.stereotype.Component;"));
    assertTrue(
        isAnImportTheGraphMayNotHave(
            "import eu.bbmri_eric.negotiator.common.exceptions.WrongRequestException;"));
    assertTrue(
        isAnImportTheGraphMayNotHave("import static org.assertj.core.api.Assertions.assertThat;"),
        "a static import names a type just as surely as a single one");

    assertTrue(
        isAnImportTheGraphMayNotHave("import com.fasterxml.jackson.databind.*;"),
        "a wildcard import names a whole package, and was the cheapest way past this rule");

    assertFalse(isAnImportTheGraphMayNotHave("import java.util.List;"));
    assertFalse(isAnImportTheGraphMayNotHave("import lombok.NonNull;"));
    assertFalse(isAnImportTheGraphMayNotHave("import java.util.*;"));
    assertFalse(isAnImportTheGraphMayNotHave("import static java.util.Map.entry;"));
    assertFalse(
        isAnImportTheGraphMayNotHave("    return importantThing.get();"),
        "the rule reads import statements, not every line with the word inside a name");
  }

  /**
   * The scan reads enough files. There are two floors because two rules read the graph package
   * alone and would check nothing while the combined count still looked fine.
   */
  @Test
  @DisplayName("the scan reads both packages, and the graph package on its own")
  void theScan_readsBothPackagesAndTheGraphPackageOnItsOwn() {
    List<Path> scanned = sources(anySource());
    assertTrue(
        scanned.size() >= MINIMUM_SCANNED_SOURCES,
        ("the purity scan found only %d sources under the graph and evaluation packages, expected"
                + " at least %d, so it is not scanning what it claims to")
            .formatted(scanned.size(), MINIMUM_SCANNED_SOURCES));

    List<Path> graphSources = sources(graphSource());
    assertTrue(
        graphSources.size() >= MINIMUM_SCANNED_GRAPH_SOURCES,
        ("the purity scan found only %d sources under the graph package, expected at least %d. The"
                + " allowlist and the evaluation-edge rule read this package alone, and would pass"
                + " by reading none of it")
            .formatted(graphSources.size(), MINIMUM_SCANNED_GRAPH_SOURCES));

    assertFalse(
        scan(graphSource(), line -> IMPORT_STATEMENT.matcher(line).find()).isEmpty(),
        "the allowlist rule found no import at all under "
            + GRAPH_PACKAGE
            + ", so its green result outside the allowlist means nothing either");
  }

  private static boolean couldReachADatabase(String line) {
    return PERSISTENCE_PACKAGES.stream().anyMatch(line::contains)
        || PERSISTENCE_TYPE.matcher(line).find()
        || REPOSITORY_TYPE.matcher(line).find();
  }

  private static boolean namesTheDefinitionPackage(String line) {
    return line.contains(DEFINITION_PACKAGE);
  }

  private static boolean namesTheEvaluationPackage(String line) {
    return line.contains(EVALUATION_PACKAGE);
  }

  private static boolean isAnImportTheGraphMayNotHave(String line) {
    Matcher imported = IMPORT_STATEMENT.matcher(line);
    return imported.find()
        && IMPORTS_PERMITTED_IN_GRAPH.stream().noneMatch(imported.group(1)::startsWith);
  }

  private static Predicate<Path> anySource() {
    return path ->
        path.toString().contains(GRAPH_PACKAGE_PATH)
            || path.toString().contains(EVALUATION_PACKAGE_PATH);
  }

  private static Predicate<Path> graphSource() {
    return path -> path.toString().contains(GRAPH_PACKAGE_PATH);
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
        if (Files.isDirectory(module.resolve("src/main/java").resolve(GRAPH_PACKAGE_PATH))
            && Files.isDirectory(
                module.resolve("src/main/java").resolve(EVALUATION_PACKAGE_PATH))) {
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
