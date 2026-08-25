package eu.bbmri_eric.negotiator.lifecycle.definition;

import static org.junit.jupiter.api.Assertions.assertFalse;
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
 * Proves the Lifecycle Definition schema is inert: no production code path reads it.
 *
 * <p>That is the whole justification for landing this schema on its own. Additive DDL and a package
 * nothing calls are harmless if they ship alone, and stay harmless only for as long as nothing
 * reaches into them. The claim degrades the moment someone finds the tables convenient, so it is
 * kept as a test rather than as a sentence in a pull request.
 *
 * <p>Three rules, because a read can be spelled three ways:
 *
 * <ul>
 *   <li>the <em>package rule</em> catches every compiled reference. The types are package-private,
 *       so code outside the package can only reach them through an import or a fully-qualified
 *       name, and both spell the package out. It also catches what package-privacy cannot: a class
 *       name in a reflection call, a component-scan base package, a JPA configuration string.
 *   <li>the <em>type rule</em> catches a Java type name where no import is involved - an entity
 *       name in a JPQL string, or a later slice that makes a type public and drops it into a
 *       signature.
 *   <li>the <em>table rule</em> catches a read that never mentions Java at all. A native query
 *       spells the table, not the entity, and this repository already has five places that issue
 *       one. Without this rule a {@code @Query(nativeQuery = true, value = "select * from
 *       lifecycle_definition")} on any existing repository passes the other two rules green while
 *       doing exactly what the slab promised not to do.
 * </ul>
 *
 * <p>Unlike {@code CharacterizationImportGuardTest}, this guard needs no exemption for itself: it
 * scans production sources and lives in the test tree, so it can name what it forbids. It borrows
 * that guard's scaffolding - the working-dir-resolved scan root, the comment-blanking reader, the
 * violation report - deliberately by copy rather than by extraction, because the two guards have
 * different lifetimes and each is meant to be deleted whole.
 */
class DefinitionInertnessGuardTest {

  private static final String DEFINITION_PACKAGE = "eu.bbmri_eric.negotiator.lifecycle.definition";

  private static final String DEFINITION_PACKAGE_PATH = DEFINITION_PACKAGE.replace('.', '/');

  private static final String PRODUCTION_SOURCE_PATH = "src/main/java";

  private static final String MIGRATION_PATH = "src/main/resources/db/migration";

  /**
   * The lowest plausible size of this repository's production tree, well under today's 431 files. A
   * threshold this loose still fails the way that matters - a scan root that resolved to the wrong
   * directory, or a walk that silently returned nothing.
   */
  private static final int MINIMUM_PRODUCTION_SOURCES = 100;

  private static final Pattern DEFINITION_PACKAGE_REFERENCE =
      Pattern.compile(Pattern.quote(DEFINITION_PACKAGE));

  /**
   * Definition types whose simple names exist nowhere else in this repository, so a bare occurrence
   * outside the package is a reference by definition. Repositories are listed alongside their
   * entities because a word-boundary match on {@code LifecycleDefinition} does not cover {@code
   * LifecycleDefinitionRepository}.
   */
  private static final List<String> DISTINCTIVE_TYPE_NAMES =
      List.of(
          "LifecycleDefinition",
          "LifecycleDefinitionRepository",
          "StateRepository",
          "EventRepository",
          "TransitionRepository",
          "GuardWiring",
          "GuardWiringRepository",
          "ActionWiring",
          "ActionWiringRepository",
          "DefinitionScope",
          "RequiredAuthority",
          "DefinitionResolver",
          "DefinitionResolverImpl",
          "DefinitionResolutionException");

  /**
   * Definition tables whose names exist nowhere else in this repository. A word boundary keeps
   * {@code lifecycle_definition} from matching {@code lifecycle_definition_id}, which is the right
   * split: the pin column on {@code negotiation} may be read, the table it points at may not.
   */
  private static final List<String> DISTINCTIVE_TABLE_NAMES =
      List.of("lifecycle_definition", "guard_wiring", "action_wiring");

  /**
   * The three definition types whose simple names are already taken in production code - {@code
   * State} and {@code Transition} by {@code org.springframework.statemachine}, {@code Event} by the
   * string literal in {@code EventListener}. Their table names are worse: {@code state}, {@code
   * event} and {@code transition} are ordinary English words and ordinary variable names.
   *
   * <p>They are exempt from the bare forms of the type and table rules, because a rule that reports
   * matches which are not references gets silenced rather than obeyed. They are not unguarded.
   * Every compiled reference is caught by the package rule, which is the only way one can be
   * written while the types are package-private, and every SQL read is caught by {@link
   * #TABLE_IN_A_SQL_CLAUSE}, which requires the name to sit where only a table can.
   */
  private static final List<String> NAMES_TOO_COMMON_TO_FORBID_BARE =
      List.of("State", "Event", "Transition");

  /**
   * The exempt table names in the one position that makes them unambiguously tables. This is what
   * the bare-name exemption above gives back: {@code from state}, {@code join event}, {@code insert
   * into transition} and {@code update state} are all caught, while a variable called {@code state}
   * is not.
   */
  private static final Pattern TABLE_IN_A_SQL_CLAUSE =
      Pattern.compile(
          "\\b(?:from|join|into|update)\\s+\"?(?:state|event|transition)\"?\\b",
          Pattern.CASE_INSENSITIVE);

  @Test
  @DisplayName("no production source outside the definition package names the definition package")
  void productionCode_doesNotNameTheDefinitionPackage() {
    List<Violation> violations =
        scan(
            outsideTheDefinitionPackage(), DefinitionInertnessGuardTest::namesTheDefinitionPackage);

    if (!violations.isEmpty()) {
      fail(
          report(
              violations,
              "The Lifecycle Definition schema must stay unread by production code",
              """
              This slab lands additive DDL and an entity package on the argument that they change
              no behaviour. A production import, fully-qualified name, reflection lookup or scan
              configuration naming %s breaks that argument, and the schema can no longer be
              reviewed as harmless on its own.

              Reading the definition tables is a later slab's work, behind DefinitionResolver.
              If that slab has started, this guard is what it deletes first, deliberately."""
                  .formatted(DEFINITION_PACKAGE)));
    }
  }

  @Test
  @DisplayName("no production source outside the definition package names a definition type")
  void productionCode_doesNotNameADefinitionType() {
    List<Violation> violations =
        scan(outsideTheDefinitionPackage(), DefinitionInertnessGuardTest::namesADefinitionType);

    if (!violations.isEmpty()) {
      fail(
          report(
              violations,
              "The Lifecycle Definition types must be named nowhere but their own package",
              """
              A simple name reaching production code without an import means a string reference -
              an entity name in a query, or a type this slab kept package-private and a later
              change made public. Either way the schema is being read, which this slab promised
              it would not be.

              The pin columns on Negotiation and NegotiationResourceLink are plain Long ids for
              exactly this reason: an id names no type and traverses into no graph."""));
    }
  }

  @Test
  @DisplayName("no production source outside the definition package reads a definition table")
  void productionCode_doesNotReadADefinitionTable() {
    List<Violation> violations =
        scan(outsideTheDefinitionPackage(), DefinitionInertnessGuardTest::readsADefinitionTable);

    if (!violations.isEmpty()) {
      fail(
          report(
              violations,
              "The Lifecycle Definition tables must be read by nothing",
              """
              A native query names the table rather than the entity, so it slips past every rule
              that looks for Java. The tables are still the new schema, and reading one is the
              failure this slab exists to rule out.

              Reading a pin column is fine and deliberately still passes: lifecycle_definition_id
              on negotiation and negotiation_resource_link is an id, not the table it points at."""));
    }
  }

  @Test
  @DisplayName("each rule matches the reference it forbids, and spares the ones it must allow")
  void theRules_matchWhatTheyForbidAndNothingElse() {
    assertTrue(
        namesTheDefinitionPackage("import %s.LifecycleDefinition;".formatted(DEFINITION_PACKAGE)),
        "The package rule must catch an import of a definition type.");
    assertTrue(
        namesADefinitionType("String q = \"SELECT d FROM LifecycleDefinition d\";"),
        "The type rule must catch an entity name in a query string.");
    assertTrue(
        readsADefinitionTable("@Query(nativeQuery = true, value = \"select * from state\")"),
        "The table rule must catch a native query against an exempt-named table.");
    assertTrue(
        readsADefinitionTable("jdbc.query(\"select active from lifecycle_definition\", mapper);"),
        "The table rule must catch a native query against a distinctively named table.");

    assertFalse(
        namesADefinitionType("private Long lifecycleDefinitionId;"),
        "The pin column's field is an id, not a type reference, and must keep passing.");
    assertFalse(
        readsADefinitionTable("select lifecycle_definition_id from negotiation"),
        "Reading a pin column is not reading the table it points at, and must keep passing.");
    assertFalse(
        readsADefinitionTable("State<String, String> state = machine.getInitialState();"),
        "A variable called state is not a table, or the rule would be silenced rather than obeyed.");
  }

  @Test
  @DisplayName("the guard scans a real production tree, and the exclusion is what makes it green")
  void guard_scansTheTreeItClaimsToProtect() {
    List<Path> sources = sourceFiles();
    assertTrue(
        sources.size() >= MINIMUM_PRODUCTION_SOURCES,
        "Only %d production sources found under %s; the guard must never pass by scanning nothing."
            .formatted(sources.size(), productionRoot()));

    assertFalse(
        scan(
                DefinitionInertnessGuardTest::isInDefinitionPackage,
                DefinitionInertnessGuardTest::namesTheDefinitionPackage)
            .isEmpty(),
        """
        The package rule found no match even inside %s, whose every file declares that package.
        The rule matches nothing, so its green result outside the package means nothing either."""
            .formatted(DEFINITION_PACKAGE));
  }

  @Test
  @DisplayName("the guard forbids only names that still exist")
  void guard_forbidsOnlyNamesThatStillExist() {
    List<String> missingTypes =
        Stream.concat(DISTINCTIVE_TYPE_NAMES.stream(), NAMES_TOO_COMMON_TO_FORBID_BARE.stream())
            .filter(name -> !Files.isRegularFile(definitionPackageRoot().resolve(name + ".java")))
            .toList();
    assertTrue(
        missingTypes.isEmpty(),
        """
        %s no longer exist in %s. A renamed type leaves this guard forbidding a name that cannot
        occur, and the type it was renamed to unguarded. Update the lists above."""
            .formatted(missingTypes, DEFINITION_PACKAGE));

    String migrations = migrationText();
    List<String> missingTables =
        Stream.concat(
                DISTINCTIVE_TABLE_NAMES.stream(),
                NAMES_TOO_COMMON_TO_FORBID_BARE.stream()
                    .map(name -> name.toLowerCase(java.util.Locale.ROOT)))
            .filter(table -> !migrations.contains("CREATE TABLE " + table))
            .toList();
    assertTrue(
        missingTables.isEmpty(),
        """
        %s are forbidden by the table rule but created by no migration under %s. Either a table
        was renamed and the rule now guards nothing, or it names a table this slab never added."""
            .formatted(missingTables, MIGRATION_PATH));
  }

  private static boolean namesTheDefinitionPackage(String line) {
    return DEFINITION_PACKAGE_REFERENCE.matcher(line).find();
  }

  private static boolean namesADefinitionType(String line) {
    return DISTINCTIVE_TYPE_NAMES.stream()
        .anyMatch(name -> wordBoundary(name).matcher(line).find());
  }

  private static boolean readsADefinitionTable(String line) {
    return DISTINCTIVE_TABLE_NAMES.stream()
            .anyMatch(table -> wordBoundary(table).matcher(line).find())
        || TABLE_IN_A_SQL_CLAUSE.matcher(line).find();
  }

  private static Predicate<Path> outsideTheDefinitionPackage() {
    return file -> !isInDefinitionPackage(file);
  }

  private static List<Violation> scan(
      Predicate<Path> fileIsSubjectToRule, Predicate<String> lineBreaksRule) {
    List<Violation> violations = new ArrayList<>();
    for (Path file : sourceFiles()) {
      if (!fileIsSubjectToRule.test(file)) {
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

  private static Pattern wordBoundary(String name) {
    return Pattern.compile("\\b" + Pattern.quote(name) + "\\b");
  }

  private static boolean isInDefinitionPackage(Path file) {
    return file.toAbsolutePath().startsWith(definitionPackageRoot());
  }

  private static Path definitionPackageRoot() {
    return productionRoot().resolve(DEFINITION_PACKAGE_PATH);
  }

  private static List<Path> sourceFiles() {
    try (Stream<Path> tree = Files.walk(productionRoot())) {
      return tree.filter(Files::isRegularFile)
          .filter(file -> file.getFileName().toString().endsWith(".java"))
          .sorted()
          .toList();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /** Every migration as one string, for asking whether a forbidden table name is still created. */
  private static String migrationText() {
    Path root = moduleRoot().resolve(MIGRATION_PATH);
    try (Stream<Path> tree = Files.walk(root)) {
      return tree.filter(Files::isRegularFile)
          .filter(file -> file.getFileName().toString().endsWith(".sql"))
          .sorted()
          .map(DefinitionInertnessGuardTest::readLines)
          .flatMap(List::stream)
          .reduce(new StringBuilder(), (text, line) -> text.append(line).append('\n'), (a, b) -> a)
          .toString();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * The two trees this guard reads live under one module, resolved from the working directory
   * rather than the classpath: the rules are about source text, and a compiled class no longer
   * shows an import. Surefire runs with the module directory as working directory; the walk upwards
   * keeps the guard working when tests are launched from the repository root or from an IDE.
   *
   * <p>Only {@code src/main/java} is scanned for violations. {@code src/main/resources} is
   * deliberately outside the walk, because this slab's own migrations name every one of these
   * tables and exempting {@code db/migration} would be the first exemption nobody re-reads. A
   * definition package named from YAML would escape this guard.
   */
  private static Path moduleRoot() {
    Path start = Path.of("").toAbsolutePath();
    for (Path candidate = start; candidate != null; candidate = candidate.getParent()) {
      for (String modulePrefix : List.of("", "backend")) {
        Path module = candidate.resolve(modulePrefix);
        if (Files.isDirectory(
                module.resolve(PRODUCTION_SOURCE_PATH).resolve(DEFINITION_PACKAGE_PATH))
            && Files.isDirectory(module.resolve(MIGRATION_PATH))) {
          return module;
        }
      }
    }
    throw new IllegalStateException(
        ("Could not locate a module holding both %s/%s and %s from working directory %s."
                + " The guard must never pass by finding nothing.")
            .formatted(PRODUCTION_SOURCE_PATH, DEFINITION_PACKAGE_PATH, MIGRATION_PATH, start));
  }

  private static Path productionRoot() {
    return moduleRoot().resolve(PRODUCTION_SOURCE_PATH);
  }

  /**
   * The file's lines with comments blanked out, so that prose explaining a forbidden name is not
   * itself a violation. Line numbering is preserved. A {@code //} inside a string literal truncates
   * the line, as it does in the guard this one is modelled on; the effect is to under-report, never
   * to invent a violation.
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
