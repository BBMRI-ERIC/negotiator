package eu.bbmri_eric.negotiator.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationEvent;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationLifecycleService;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationStateChangeEvent;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.NegotiationResourceEvent;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.NegotiationResourceState;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.ResourceLifecycleService;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.ResourceStateChangeEvent;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The slab gate for <em>Decouple consumers from the Lifecycle enums</em>, in executable form.
 *
 * <p>Map ticket 07 states the gate as a sentence: the four Lifecycle enums may be referenced
 * <em>only</em> inside {@code negotiation/state_machine/} and the three metadata DTOs. A sentence
 * in a pull request is checked once and rots from the next commit onwards, so it is kept here as a
 * test instead - which is what makes "decoupled" a fact the build re-establishes rather than a
 * claim somebody made.
 *
 * <p><b>Two rules, because coupling can be spelled two ways.</b>
 *
 * <ul>
 *   <li>The <em>identifier rule</em> reads every production source as text and fails on a
 *       whole-word occurrence of an enum's name outside the two exemptions. This is the rule that
 *       catches an import, a field type, a fully-qualified name, a {@code switch} subject and a
 *       class literal - everything a consumer can write that ties it to the closed set.
 *   <li>The <em>signature rule</em> is the one the identifier scan cannot express, and the reason
 *       this is a test rather than a grep. A consumer can call {@code event.getToState().name()}
 *       and import nothing: no enum name appears anywhere in its source, and the identifier rule
 *       reports green over a file that will not compile once the enum is deleted. So the four seam
 *       types are inspected reflectively instead, and every type they mention in a signature has to
 *       be free of the enums. This is slab 08's lesson in mirror image - its table rule went green
 *       over a read that never named Java - and that slab wrote it down for this one.
 * </ul>
 *
 * <p><b>Word boundaries are load-bearing.</b> {@code NegotiationStateChangeEvent} and {@code
 * ResourceStateChangeEvent} are the application events this slab converted, they contain {@code
 * NegotiationState} as a prefix, and consumers name them constantly and legitimately. A substring
 * scan would report every one of those consumers as a violation, and a rule that reports matches
 * which are not references gets silenced rather than obeyed. {@code
 * CharacterizationImportGuardTest} documents the same trap from the test side.
 *
 * <p><b>The vocabulary comes from the enums, not from a hand-kept list.</b> The forbidden names are
 * the four {@code Class} objects' simple names, so this guard cannot forbid a name the Lifecycle no
 * longer uses, nor miss one it renamed.
 *
 * <p><b>This guard is deleted at cutover, together with the enums it forbids naming.</b> Once the
 * Transition Evaluator replaces Spring Statemachine there is no closed set left to be coupled to,
 * the {@code negotiation.state_machine} package goes whole, and the four imports above stop
 * compiling - loudly, at the one moment the question this guard asks stops having an answer. Delete
 * the class then; do not repoint it at something else.
 *
 * <p>The scanning technique is borrowed from {@code CharacterizationImportGuardTest} and {@code
 * DefinitionInertnessGuardTest} - a working-directory-resolved scan root, a reader that blanks
 * comments so prose naming a forbidden identifier is not itself a violation, and a violation report
 * carrying file and line. As slab 08 recorded for those two and slice 03 for {@code
 * RawStateNamesInSqlGuardTest}, the copy is deliberate: these guards have different lifetimes and
 * each is meant to be deleted whole.
 */
class LifecycleEnumDecouplingGuardTest {

  private static final String PRODUCTION_SOURCE_PATH = "src/main/java";

  private static final String NEGOTIATOR_PACKAGE_PATH = "eu/bbmri_eric/negotiator";

  /**
   * Exemption one: the Lifecycle's own package, where the enums are declared and where every class
   * that drives Spring Statemachine lives. Naming a State by its enum is what this package is
   * <em>for</em> while the library runs, and the whole package is deleted at cutover, so nothing
   * here can outlive the closed set. {@code EnumBackedLifecycleCatalog} sits inside it for exactly
   * this reason: it answers the three questions that still need the closed set, and it answers them
   * without any consumer naming an enum.
   */
  private static final String STATE_MACHINE_PACKAGE_PATH =
      NEGOTIATOR_PACKAGE_PATH + "/negotiation/state_machine";

  /**
   * Exemption two: carve-out 1 of map ticket 07 - the three metadata DTOs, each holding one enum as
   * a single {@code value} field. They belong to ticket 04, which asks whether an endpoint
   * enumerating a <em>universe</em> of States still makes sense; carving them out is what kept this
   * slab from being blocked on that question. They are pinned by full path rather than by simple
   * name, so a fourth DTO borrowing one of these names elsewhere is not quietly exempt too.
   */
  private static final List<String> METADATA_DTO_FILES =
      List.of(
          NEGOTIATOR_PACKAGE_PATH + "/negotiation/dto/NegotiationStateMetadataDto.java",
          NEGOTIATOR_PACKAGE_PATH + "/governance/resource/dto/ResourceStateMetadataDto.java",
          NEGOTIATOR_PACKAGE_PATH + "/governance/resource/dto/ResourceEventMetadataDto.java");

  /**
   * The lowest plausible size of this repository's production tree, well under today's 437 files. A
   * threshold this loose still fails the way that matters - a scan root that resolved to the wrong
   * directory, or a walk that silently returned nothing.
   */
  private static final int MINIMUM_PRODUCTION_SOURCES = 100;

  /** The closed set this slab moved every consumer off. */
  private static final List<Class<?>> LIFECYCLE_ENUMS =
      List.of(
          NegotiationState.class,
          NegotiationEvent.class,
          NegotiationResourceState.class,
          NegotiationResourceEvent.class);

  private static final List<Pattern> FORBIDDEN_IDENTIFIERS =
      LIFECYCLE_ENUMS.stream()
          .map(type -> Pattern.compile("\\b" + Pattern.quote(type.getSimpleName()) + "\\b"))
          .toList();

  /**
   * The two application events every notification handler, webhook mapping strategy and timeline
   * reader observes. Their three State-and-Event accessors are the widest-reach seam in the slab.
   */
  private static final List<Class<?>> APPLICATION_EVENTS =
      List.of(NegotiationStateChangeEvent.class, ResourceStateChangeEvent.class);

  /** The accessors a consumer reads a State or an Event off, on both application events. */
  private static final List<String> STATE_CHANGE_ACCESSORS =
      List.of("getFromState", "getToState", "getEvent");

  /** The two interfaces through which everything outside the Lifecycle drives a Lifecycle. */
  private static final List<Class<?>> LIFECYCLE_SERVICES =
      List.of(NegotiationLifecycleService.class, ResourceLifecycleService.class);

  @Test
  @DisplayName("no production source outside the Lifecycle names a Lifecycle enum")
  void productionCode_doesNotNameALifecycleEnum() {
    List<Violation> violations =
        scan(isSubjectToTheIdentifierRule(), LifecycleEnumDecouplingGuardTest::namesALifecycleEnum);

    if (!violations.isEmpty()) {
      fail(
          report(
              violations,
              "Outside the Lifecycle, a State or an Event must be named by a bare String",
              """
              The four Lifecycle enums are deleted by the redesign, and each of the lines above
              will stop compiling when they go. That is the single-commit, whole-application
              change this slab exists to avoid, so move the reference now instead:

                - a name some behaviour depends on existing comes from WellKnownNegotiationStates,
                  WellKnownResourceStates or WellKnownResourceEvents;
                - a name that merely travels - a DTO field, a filter value, a query parameter -
                  is data, and carries as a String off the column, the request or the Pin;
                - a label, a description, an existence check or a Resource State's ordinal comes
                  from EnumBackedLifecycleCatalog, which owns the last compile-time knowledge of
                  the closed set and dies with it.

              The only exemptions are %s (the Lifecycle's own package) and the three metadata
              DTOs of carve-out 1. Adding a fourth exemption is reopening the slab gate, not
              fixing a build."""
                  .formatted(STATE_MACHINE_PACKAGE_PATH.replace('/', '.'))));
    }
  }

  @Test
  @DisplayName("the seam's accessors and both Lifecycle services deal in String")
  void theSeam_dealsInStrings() {
    for (Class<?> applicationEvent : APPLICATION_EVENTS) {
      for (String accessor : STATE_CHANGE_ACCESSORS) {
        assertEquals(
            String.class,
            declaredMethod(applicationEvent, accessor).getReturnType(),
            """
            %s.%s() must return a String. A consumer reading a State off this accessor is coupled
            to whatever type it returns, whether or not its source ever names one."""
                .formatted(applicationEvent.getSimpleName(), accessor));
      }
    }

    assertEquals(
        "java.util.Set<java.lang.String>",
        declaredMethod(NegotiationLifecycleService.class, "getPossibleEvents", String.class)
            .getGenericReturnType()
            .getTypeName(),
        "NegotiationLifecycleService.getPossibleEvents must answer with Event names.");
    assertEquals(
        "java.util.Set<java.lang.String>",
        declaredMethod(
                ResourceLifecycleService.class, "getPossibleEvents", String.class, String.class)
            .getGenericReturnType()
            .getTypeName(),
        "ResourceLifecycleService.getPossibleEvents must answer with Event names.");
    assertEquals(
        String.class,
        declaredMethod(NegotiationLifecycleService.class, "sendEvent", String.class, String.class)
            .getReturnType(),
        "NegotiationLifecycleService.sendEvent must take an Event name and answer with a State"
            + " name.");
    assertEquals(
        String.class,
        declaredMethod(
                NegotiationLifecycleService.class,
                "sendEvent",
                String.class,
                String.class,
                String.class)
            .getReturnType(),
        "The message-carrying NegotiationLifecycleService.sendEvent must do the same.");
    assertEquals(
        String.class,
        declaredMethod(
                ResourceLifecycleService.class,
                "sendEvent",
                String.class,
                String.class,
                String.class)
            .getReturnType(),
        "ResourceLifecycleService.sendEvent must take an Event name and answer with a State name.");
  }

  @Test
  @DisplayName("no seam signature mentions a Lifecycle enum, at any depth")
  void theSeam_mentionsNoLifecycleEnumInASignature() {
    List<String> coupled = new ArrayList<>();
    for (Class<?> seamType : seamTypes()) {
      for (Method method : declaredMethods(seamType)) {
        Set<Class<?>> enums = lifecycleEnumsMentionedBy(method);
        if (!enums.isEmpty()) {
          coupled.add(
              "  %s.%s -> %s"
                  .formatted(seamType.getSimpleName(), signatureOf(method), simpleNames(enums)));
        }
      }
    }

    assertTrue(
        coupled.isEmpty(),
        """
        The Lifecycle seam still hands consumers an enum:

        %s

        This is the coupling an identifier scan cannot see. A consumer calling one of these
        methods needs no import and names no forbidden type, so the guard's other rule reports
        green over code that stops compiling the day the enums are deleted. A type argument
        counts as much as a return type: Set<NegotiationEvent> couples every caller that reads
        the set.

        Convert the signature to String and translate inside the Lifecycle, where the closed set
        is still honest for as long as Spring Statemachine runs."""
            .formatted(String.join("\n", coupled)));
  }

  @Test
  @DisplayName("the signature rule finds an enum wherever a signature can hide one")
  void theSignatureRule_matchesWhatItForbidsAndNothingElse() {
    assertEquals(
        Set.of(NegotiationEvent.class),
        lifecycleEnumsMentionedBy(
            declaredMethod(CoupledSeamFixture.class, "possibleEvents", String.class)),
        "The signature rule must see an enum that only ever appears as a type argument.");
    assertEquals(
        Set.of(NegotiationResourceState.class),
        lifecycleEnumsMentionedBy(declaredMethod(CoupledSeamFixture.class, "state")),
        "The signature rule must see an enum returned outright.");
    assertEquals(
        Set.of(NegotiationResourceEvent.class),
        lifecycleEnumsMentionedBy(
            declaredMethod(CoupledSeamFixture.class, "send", NegotiationResourceEvent.class)),
        "The signature rule must see an enum that only appears in a parameter.");

    assertTrue(
        lifecycleEnumsMentionedBy(declaredMethod(NegotiationStateChangeEvent.class, "getToState"))
            .isEmpty(),
        "An accessor that answers with a name mentions no enum, or the rule reports the seam it"
            + " is meant to clear.");
  }

  @Test
  @DisplayName("the identifier rule catches an enum reference and spares the application events")
  void theIdentifierRule_matchesWhatItForbidsAndNothingElse() {
    assertTrue(
        namesALifecycleEnum(
            "import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation"
                + ".NegotiationState;"),
        "The identifier rule must catch an import of a Lifecycle enum.");
    assertTrue(
        namesALifecycleEnum("  private NegotiationResourceState currentState;"),
        "The identifier rule must catch a field declared as a Lifecycle enum.");
    assertTrue(
        namesALifecycleEnum("    return NegotiationEvent.valueOf(name);"),
        "The identifier rule must catch a static reference no import is needed for.");

    assertFalse(
        namesALifecycleEnum("  public void handle(NegotiationStateChangeEvent event) {"),
        """
        NegotiationStateChangeEvent is an application event consumers are expected to name, and
        it contains NegotiationState as a prefix. A rule that reports it reports most of this
        slab's consumers and gets silenced rather than obeyed.""");
    assertFalse(
        namesALifecycleEnum("  public void handle(ResourceStateChangeEvent event) {"),
        "ResourceStateChangeEvent is the other one, and must be spared for the same reason.");
    assertFalse(
        namesALifecycleEnum("  private String negotiationState;"),
        "A String field named after a State is what this slab converted things to, not a"
            + " violation.");
  }

  @Test
  @DisplayName("the guard scans a real production tree, and the exemptions are what make it green")
  void guard_scansTheTreeItClaimsToProtect() {
    List<Path> sources = sourceFiles();
    assertTrue(
        sources.size() >= MINIMUM_PRODUCTION_SOURCES,
        "Only %d production sources found under %s; the guard must never pass by scanning nothing."
            .formatted(sources.size(), productionRoot()));

    assertFalse(
        scan(
                LifecycleEnumDecouplingGuardTest::isInStateMachinePackage,
                LifecycleEnumDecouplingGuardTest::namesALifecycleEnum)
            .isEmpty(),
        """
        The identifier rule found no match even inside %s, where the four enums are declared and
        used on every second line. The rule matches nothing, so its green result everywhere else
        means nothing either."""
            .formatted(STATE_MACHINE_PACKAGE_PATH));
  }

  @Test
  @DisplayName("the guard exempts only files that still exist and still need exempting")
  void guard_exemptsOnlyWhatItStillHasTo() {
    Path stateMachineRoot = productionRoot().resolve(STATE_MACHINE_PACKAGE_PATH);
    assertTrue(
        Files.isDirectory(stateMachineRoot),
        """
        The exempted package %s is not there. If Spring Statemachine has been replaced, this whole
        guard goes with it; if the package merely moved, an entire tree of enum references is
        currently covered by no rule at all."""
            .formatted(STATE_MACHINE_PACKAGE_PATH));

    for (String dto : METADATA_DTO_FILES) {
      Path file = productionRoot().resolve(dto);
      assertTrue(
          Files.isRegularFile(file),
          """
          The exempted metadata DTO %s no longer exists. If ticket 04 moved or renamed it, remove
          this exemption in the same change - otherwise the guard exempts a file that is not
          there, and whatever replaced it is unguarded."""
              .formatted(dto));
      assertFalse(
          scan(
                  candidate -> candidate.equals(file),
                  LifecycleEnumDecouplingGuardTest::namesALifecycleEnum)
              .isEmpty(),
          """
          The exempted metadata DTO %s no longer names a Lifecycle enum, so the exemption buys
          nothing and hides everything. Delete its entry from METADATA_DTO_FILES: an exemption
          nobody can tell is unnecessary is exactly the kind a reviewer stops reading."""
              .formatted(dto));
    }
  }

  /**
   * A fixture, not a rule, and the only thing standing between the signature rule and the hole
   * slice 11 found in slice 03's guard: a detector exercised solely against a tree that no longer
   * contains an example reports green whether it works or not. Today's seam mentions no enum
   * anywhere - that is the property being asserted - so the three shapes it must catch are declared
   * here instead, one per way a signature can carry an enum past an identifier scan. Nothing scans
   * this interface and nothing implements it.
   */
  private interface CoupledSeamFixture {

    Set<NegotiationEvent> possibleEvents(String negotiationId);

    NegotiationResourceState state();

    String send(NegotiationResourceEvent event);
  }

  private static boolean namesALifecycleEnum(String line) {
    return FORBIDDEN_IDENTIFIERS.stream().anyMatch(name -> name.matcher(line).find());
  }

  private static Predicate<Path> isSubjectToTheIdentifierRule() {
    return file -> !isInStateMachinePackage(file) && !isAnExemptMetadataDto(file);
  }

  private static boolean isInStateMachinePackage(Path file) {
    return file.toAbsolutePath().startsWith(productionRoot().resolve(STATE_MACHINE_PACKAGE_PATH));
  }

  private static boolean isAnExemptMetadataDto(Path file) {
    return METADATA_DTO_FILES.stream()
        .anyMatch(dto -> file.toAbsolutePath().equals(productionRoot().resolve(dto)));
  }

  private static List<Class<?>> seamTypes() {
    return Stream.concat(APPLICATION_EVENTS.stream(), LIFECYCLE_SERVICES.stream()).toList();
  }

  /**
   * The methods a caller can actually see, which is not quite what {@code getDeclaredMethods}
   * returns under the suite: JaCoCo instruments every loaded class with a synthetic {@code
   * $jacocoInit}, and a covariant override leaves a bridge behind. Slice 01 recorded this after a
   * reflection assertion passed under a plain compile and failed under coverage.
   */
  private static List<Method> declaredMethods(Class<?> type) {
    return Arrays.stream(type.getDeclaredMethods())
        .filter(method -> !method.isSynthetic() && !method.isBridge())
        .sorted(Comparator.comparing(Method::getName).thenComparing(Method::getParameterCount))
        .toList();
  }

  private static Method declaredMethod(Class<?> type, String name, Class<?>... parameters) {
    try {
      return type.getDeclaredMethod(name, parameters);
    } catch (NoSuchMethodException e) {
      throw new AssertionError(
          """
          %s.%s(%s) does not exist. This guard names the seam methods explicitly, so a rename
          leaves the accessor consumers actually call covered by nothing. Update the names above,
          or delete the guard if the seam itself is gone."""
              .formatted(type.getSimpleName(), name, simpleNames(List.of(parameters))),
          e);
    }
  }

  private static Set<Class<?>> lifecycleEnumsMentionedBy(Method method) {
    List<Type> declared = new ArrayList<>();
    declared.add(method.getGenericReturnType());
    declared.addAll(List.of(method.getGenericParameterTypes()));
    declared.addAll(List.of(method.getGenericExceptionTypes()));

    Set<Class<?>> mentioned = new LinkedHashSet<>();
    Set<Type> seen = new LinkedHashSet<>();
    for (Type type : declared) {
      collectClasses(type, seen, mentioned);
    }
    mentioned.retainAll(LIFECYCLE_ENUMS);
    return mentioned;
  }

  /**
   * Every class a generic type mentions, at any depth. A method returning {@code
   * Set<NegotiationEvent>} couples its callers exactly as hard as one returning the enum outright,
   * and the raw {@code getReturnType()} sees only {@code Set}. Bounds and wildcards are walked for
   * the same reason; {@code seen} keeps a self-referential type variable from looping.
   */
  private static void collectClasses(Type type, Set<Type> seen, Set<Class<?>> collected) {
    if (type == null || !seen.add(type)) {
      return;
    }
    switch (type) {
      case Class<?> raw -> {
        collected.add(raw);
        if (raw.isArray()) {
          collectClasses(raw.getComponentType(), seen, collected);
        }
      }
      case ParameterizedType parameterized -> {
        collectClasses(parameterized.getRawType(), seen, collected);
        for (Type argument : parameterized.getActualTypeArguments()) {
          collectClasses(argument, seen, collected);
        }
      }
      case GenericArrayType array ->
          collectClasses(array.getGenericComponentType(), seen, collected);
      case WildcardType wildcard -> {
        for (Type bound : wildcard.getUpperBounds()) {
          collectClasses(bound, seen, collected);
        }
        for (Type bound : wildcard.getLowerBounds()) {
          collectClasses(bound, seen, collected);
        }
      }
      case TypeVariable<?> variable -> {
        for (Type bound : variable.getBounds()) {
          collectClasses(bound, seen, collected);
        }
      }
      default -> {
        // No other Type implementation can name a class, so there is nothing to collect.
      }
    }
  }

  private static String signatureOf(Method method) {
    return "%s(%s) : %s"
        .formatted(
            method.getName(),
            String.join(
                ", ",
                Arrays.stream(method.getGenericParameterTypes()).map(Type::getTypeName).toList()),
            method.getGenericReturnType().getTypeName());
  }

  private static String simpleNames(Iterable<Class<?>> types) {
    List<String> names = new ArrayList<>();
    types.forEach(type -> names.add(type.getSimpleName()));
    return String.join(", ", names);
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

  private static Path productionRoot() {
    return moduleRoot().resolve(PRODUCTION_SOURCE_PATH);
  }

  /**
   * Resolves the production tree from the working directory rather than the classpath, because the
   * identifier rule is about source text and a compiled class no longer shows an import. Surefire
   * runs with the module directory as working directory; the walk upwards keeps the guard working
   * when tests are launched from the repository root or from an IDE.
   */
  private static Path moduleRoot() {
    Path start = Path.of("").toAbsolutePath();
    for (Path candidate = start; candidate != null; candidate = candidate.getParent()) {
      for (String modulePrefix : List.of("", "backend")) {
        Path module = candidate.resolve(modulePrefix);
        if (Files.isDirectory(
            module.resolve(PRODUCTION_SOURCE_PATH).resolve(NEGOTIATOR_PACKAGE_PATH))) {
          return module;
        }
      }
    }
    throw new IllegalStateException(
        ("Could not locate a module holding %s/%s from working directory %s."
                + " The guard must never pass by finding nothing.")
            .formatted(PRODUCTION_SOURCE_PATH, NEGOTIATOR_PACKAGE_PATH, start));
  }

  /**
   * The file's lines with comments blanked out, so that prose explaining a forbidden name is not
   * itself a violation - which matters more here than in the guards this is copied from, because a
   * converted consumer may well carry a javadoc line saying which enum it used to name. Line
   * numbering is preserved. A {@code //} inside a string literal truncates the line, as it does in
   * those guards; the effect is to under-report, never to invent a violation.
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
