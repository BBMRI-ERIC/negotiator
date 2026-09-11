package eu.bbmri_eric.negotiator.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
 * Fails the build if production code outside {@code negotiation/state_machine/} and the three
 * metadata DTOs references one of the four Lifecycle enums.
 *
 * <p>It applies two rules, because code can couple to an enum in two ways.
 *
 * <ul>
 *   <li>The <em>identifier rule</em> reads every production source as text and fails on a
 *       whole-word occurrence of an enum's name outside the exemptions. It catches imports, field
 *       types, fully qualified names, {@code switch} subjects and class literals.
 *   <li>The <em>signature rule</em> covers what a text scan cannot. A consumer can call {@code
 *       event.getToState().name()} without naming any enum, and that code stops compiling once the
 *       enum is deleted. So the rule inspects the four seam types by reflection, and no type in
 *       their signatures may mention an enum.
 * </ul>
 *
 * <p>The identifier rule matches whole words. {@code NegotiationStateChangeEvent} and {@code
 * ResourceStateChangeEvent} start with {@code NegotiationState}, and consumers name them all the
 * time. A substring scan would report each of those consumers.
 *
 * <p>The forbidden names come from the four enums' {@code Class} objects, so a renamed enum cannot
 * slip past the guard.
 *
 * <p>Delete this guard at cutover, together with the enums. When the Transition Evaluator replaces
 * Spring Statemachine, the {@code negotiation.state_machine} package goes and the imports above
 * stop compiling. Do not point the guard at something else.
 *
 * <p>The scanning code is copied between the guard tests on purpose. The guards have different
 * lifetimes and each is deleted whole.
 */
class LifecycleEnumDecouplingGuardTest {

  private static final String PRODUCTION_SOURCE_PATH = "src/main/java";

  private static final String NEGOTIATOR_PACKAGE_PATH = "eu/bbmri_eric/negotiator";

  /**
   * The Lifecycle's own package, where the enums are declared and Spring Statemachine runs. The
   * whole package is deleted at cutover. {@code EnumBackedLifecycleCatalog} lives here so consumers
   * can ask about the enums without naming them.
   */
  private static final String STATE_MACHINE_PACKAGE_PATH =
      NEGOTIATOR_PACKAGE_PATH + "/negotiation/state_machine";

  /**
   * The three metadata DTOs, each holding one enum as its {@code value} field. They stay exempt
   * until someone decides whether an endpoint listing every State still makes sense. They are
   * listed by full path, so another file with the same simple name is not exempt.
   */
  private static final List<String> METADATA_DTO_FILES =
      List.of(
          NEGOTIATOR_PACKAGE_PATH + "/negotiation/dto/NegotiationStateMetadataDto.java",
          NEGOTIATOR_PACKAGE_PATH + "/governance/resource/dto/ResourceStateMetadataDto.java",
          NEGOTIATOR_PACKAGE_PATH + "/governance/resource/dto/ResourceEventMetadataDto.java");

  /**
   * Well below the real size of the production tree. It still fails when the scan root resolves to
   * the wrong directory or the walk returns nothing.
   */
  private static final int MINIMUM_PRODUCTION_SOURCES = 100;

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
   * The two application events that notification handlers, webhook mapping strategies and timeline
   * readers observe.
   */
  private static final List<Class<?>> APPLICATION_EVENTS =
      List.of(NegotiationStateChangeEvent.class, ResourceStateChangeEvent.class);

  private static final List<String> STATE_CHANGE_ACCESSORS =
      List.of("getFromState", "getToState", "getEvent");

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
              The four Lifecycle enums are deleted at cutover, and each of the lines above will
              stop compiling when they go. Move the reference now instead:

                - a name some behaviour depends on existing comes from WellKnownNegotiationStates,
                  WellKnownResourceStates or WellKnownResourceEvents;
                - a name that is only passed along, such as a DTO field, a filter value or a
                  query parameter, is data and travels as a String from the column, the request
                  or the Pin;
                - a label, a description, an existence check or a Resource State's ordinal comes
                  from EnumBackedLifecycleCatalog, which is deleted together with the enums.

              The only exemptions are %s (the Lifecycle's own package) and the three metadata
              DTOs. Do not add an exemption to make the build pass."""
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

    assertAnswersWithAName(
        declaredMethod(NegotiationLifecycleService.class, "sendEvent", String.class, String.class));
    assertAnswersWithAName(
        declaredMethod(
            NegotiationLifecycleService.class,
            "sendEvent",
            String.class,
            String.class,
            String.class));
    assertAnswersWithAName(
        declaredMethod(
            ResourceLifecycleService.class, "sendEvent", String.class, String.class, String.class));

    assertAnswersWithNames(
        declaredMethod(NegotiationLifecycleService.class, "getPossibleEvents", String.class));
    assertAnswersWithNames(
        declaredMethod(
            ResourceLifecycleService.class, "getPossibleEvents", String.class, String.class));
  }

  private static void assertAnswersWithAName(Method method) {
    assertEquals(
        String.class,
        method.getReturnType(),
        """
        %s must answer with a name. Its callers are outside the Lifecycle and cannot be handed a
        type the cutover deletes."""
            .formatted(signatureOf(method)));
  }

  private static void assertAnswersWithNames(Method method) {
    Type returned = method.getGenericReturnType();
    assertEquals(
        Set.class,
        method.getReturnType(),
        "%s must answer with a Set.".formatted(signatureOf(method)));
    assertTrue(
        returned instanceof ParameterizedType parameterized
            && List.of(parameterized.getActualTypeArguments()).equals(List.of(String.class)),
        """
        %s must answer with names. A raw Set, or one of anything but String, hands its callers
        something the cutover deletes, and the type argument is where that hides."""
            .formatted(signatureOf(method)));
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
        it contains NegotiationState as a prefix. A rule that reports it reports most consumers
        of the Lifecycle and gets switched off.""");
    assertFalse(
        namesALifecycleEnum("  public void handle(ResourceStateChangeEvent event) {"),
        "ResourceStateChangeEvent is the other one, and must be spared for the same reason.");
    assertFalse(
        namesALifecycleEnum("  private String negotiationState;"),
        "A String field named after a State is the intended replacement, not a violation.");
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
  @DisplayName("the scan root refuses to resolve rather than finding nothing")
  void resolvingTheScanRoot_failsRatherThanFindingNothing() {
    IllegalStateException refused =
        assertThrows(
            IllegalStateException.class,
            () -> moduleHolding("src/main/nowhere", NEGOTIATOR_PACKAGE_PATH),
            """
            Asked for a source directory that does not exist, the resolution step returned a path
            instead of refusing. Everything both rules report is relative to that path, so a wrong
            one walks an empty tree, finds no reference, and reports green over a codebase it never
            looked at.""");
    assertTrue(
        refused.getMessage().contains("src/main/nowhere"),
        "The refusal must name the path it could not find, or it cannot be acted on: "
            + refused.getMessage());

    assertEquals(
        moduleRoot().resolve(PRODUCTION_SOURCE_PATH),
        productionRoot(),
        "The real scan root must still resolve through the same step the wrong one was refused by.");
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
          The exempted metadata DTO %s no longer exists. If it moved or was renamed, remove this
          exemption in the same change. Otherwise the guard exempts a file that is not there, and
          whatever replaced it is unguarded."""
              .formatted(dto));
      assertTrue(
          codeLines(file).stream().anyMatch(LifecycleEnumDecouplingGuardTest::namesALifecycleEnum),
          """
          The exempted metadata DTO %s no longer names a Lifecycle enum, so the exemption buys
          nothing. Delete its entry from METADATA_DTO_FILES."""
              .formatted(dto));
    }
  }

  /**
   * Gives the signature rule something to catch. The real seam mentions no enum, so a broken rule
   * would pass against it. Each method shows one way a signature can carry an enum past an
   * identifier scan. Nothing scans or implements this interface.
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
   * The methods a caller can see. Synthetic and bridge methods are dropped because JaCoCo adds a
   * synthetic {@code $jacocoInit} to every loaded class, and a covariant override leaves a bridge.
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
   * Set<NegotiationEvent>} couples its callers as much as one returning the enum, and {@code
   * getReturnType()} sees only {@code Set}. {@code seen} stops a self-referential type variable
   * from looping.
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

  private static Path moduleRoot() {
    return moduleHolding(PRODUCTION_SOURCE_PATH, NEGOTIATOR_PACKAGE_PATH);
  }

  /**
   * Resolves the production tree from the working directory instead of the classpath, because the
   * identifier rule reads source text and a compiled class shows no imports. Surefire runs in the
   * module directory, and walking upwards keeps the guard working from the repository root or an
   * IDE.
   *
   * <p>It takes both path parts as arguments so that {@link
   * #resolvingTheScanRoot_failsRatherThanFindingNothing()} can ask for a missing directory and
   * check that it throws.
   */
  private static Path moduleHolding(String sourcePath, String packagePath) {
    Path start = Path.of("").toAbsolutePath();
    for (Path candidate = start; candidate != null; candidate = candidate.getParent()) {
      for (String modulePrefix : List.of("", "backend")) {
        Path module = candidate.resolve(modulePrefix);
        if (Files.isDirectory(module.resolve(sourcePath).resolve(packagePath))) {
          return module;
        }
      }
    }
    throw new IllegalStateException(
        ("Could not locate a module holding %s/%s from working directory %s."
                + " The guard must never pass by finding nothing.")
            .formatted(sourcePath, packagePath, start));
  }

  /**
   * The file's lines with comments blanked out, so a comment naming an enum is not a violation.
   * Line numbers stay the same. A {@code //} inside a string literal truncates the line, which can
   * only hide a violation, never invent one.
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
