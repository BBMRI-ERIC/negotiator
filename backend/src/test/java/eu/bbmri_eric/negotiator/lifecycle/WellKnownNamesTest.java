package eu.bbmri_eric.negotiator.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.NegotiationResourceEvent;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.NegotiationResourceState;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pins the three Well-known name holders to exactly nine names, and pins those names to the
 * spelling of the Lifecycle enums.
 *
 * <p>The holders keep only names whose existence some behaviour depends on: a visibility rule that
 * excludes drafts, Spawn, and the Override Event. Every other State or Event name is data, read from
 * the column, the request or the Definition Version Pin.
 *
 * <p>The sets are exact. A holder that grows toward declaring every State is a closed enum again and
 * would have to stay in sync with seed data it cannot see, so the test asserts set equality rather
 * than containment. Adding a name means changing this test.
 *
 * <p>Every name is spelled the way the enum spells it. A typo would compile into a comparison that
 * never matches, so the names are checked against the enums while the enums exist. When the enums
 * go, these assertions stop compiling rather than leaving a silent gap.
 *
 * <p>A holder holds constants and nothing else. What a State means is a read against the Definition
 * Version, which no constant can answer.
 */
class WellKnownNamesTest {

  private static final List<Class<?>> HOLDERS =
      List.of(
          WellKnownNegotiationStates.class,
          WellKnownResourceStates.class,
          WellKnownResourceEvents.class);

  /**
   * The five Negotiation States some behaviour depends on. There is a single Negotiation-scope
   * Definition Family, so these names do not vary between definitions.
   */
  private static final Set<String> EXPECTED_NEGOTIATION_STATES =
      new LinkedHashSet<>(List.of("DRAFT", "SUBMITTED", "IN_PROGRESS", "DECLINED", "ABANDONED"));

  /**
   * The three Resource States Spawn writes. Unlike the Negotiation set, these assume a family's
   * vocabulary, and custom Resource-scope Definition Families may not use these names.
   */
  private static final Set<String> EXPECTED_RESOURCE_STATES =
      new LinkedHashSet<>(
          List.of("SUBMITTED", "REPRESENTATIVE_CONTACTED", "REPRESENTATIVE_UNREACHABLE"));

  /**
   * The Override Event, and only it. Every Definition Version carries it, because it is how an
   * admin's direct change of a State appears in history.
   */
  private static final Set<String> EXPECTED_RESOURCE_EVENTS =
      new LinkedHashSet<>(List.of("OVERRIDE"));

  @Test
  @DisplayName("the Negotiation holder carries exactly the five Well-known States")
  void wellKnownNegotiationStates_carriesExactlyTheFiveNames() {
    assertEquals(EXPECTED_NEGOTIATION_STATES, constantsOf(WellKnownNegotiationStates.class));
  }

  @Test
  @DisplayName("the Resource State holder carries exactly the three names Spawn writes")
  void wellKnownResourceStates_carriesExactlyTheThreeNames() {
    assertEquals(EXPECTED_RESOURCE_STATES, constantsOf(WellKnownResourceStates.class));
  }

  @Test
  @DisplayName("the Resource Event holder carries exactly the Override Event")
  void wellKnownResourceEvents_carriesExactlyTheOverrideEvent() {
    assertEquals(EXPECTED_RESOURCE_EVENTS, constantsOf(WellKnownResourceEvents.class));
  }

  @Test
  @DisplayName("every Well-known Negotiation State is spelled the way the enum spells it")
  void everyNegotiationStateName_matchesAnEnumConstant() {
    assertEveryNameResolves(
        constantsOf(WellKnownNegotiationStates.class),
        NegotiationState::valueOf,
        "NegotiationState");
  }

  @Test
  @DisplayName("every Well-known Resource State is spelled the way the enum spells it")
  void everyResourceStateName_matchesAnEnumConstant() {
    assertEveryNameResolves(
        constantsOf(WellKnownResourceStates.class),
        NegotiationResourceState::valueOf,
        "NegotiationResourceState");
  }

  @Test
  @DisplayName("the Override Event is spelled the way the enum spells it")
  void everyResourceEventName_matchesAnEnumConstant() {
    assertEveryNameResolves(
        constantsOf(WellKnownResourceEvents.class),
        NegotiationResourceEvent::valueOf,
        "NegotiationResourceEvent");
  }

  @Test
  @DisplayName("no holder declares the same name twice under two field names")
  void noHolder_declaresAnAliasForAName() {
    for (Class<?> holder : HOLDERS) {
      List<String> declared = declaredNamesOf(holder);
      assertEquals(
          declared.size(),
          new LinkedHashSet<>(declared).size(),
          "%s declares two constants with the same value. A set collapses an alias, so it would slip past the exact-set assertions."
              .formatted(holder.getSimpleName()));
    }
  }

  @Test
  @DisplayName("a holder holds constants only, has no behaviour, and cannot be instantiated")
  void everyHolder_isAHolderOfConstantsOnly() {
    for (Class<?> holder : HOLDERS) {
      assertTrue(
          Modifier.isFinal(holder.getModifiers()),
          "%s must be final, because a subclass could add a tenth name out of sight."
              .formatted(holder.getSimpleName()));
      assertEquals(
          List.of(),
          authoredMethodsOf(holder),
          "%s must declare no methods. A State's label, description or Progress Order is a read against the Definition Version, not something a holder of names answers."
              .formatted(holder.getSimpleName()));

      Constructor<?>[] constructors = holder.getDeclaredConstructors();
      assertEquals(
          1,
          constructors.length,
          "%s must declare exactly one constructor, and it must be the one that refuses."
              .formatted(holder.getSimpleName()));
      assertTrue(
          Modifier.isPrivate(constructors[0].getModifiers()),
          "%s must not be instantiable.".formatted(holder.getSimpleName()));

      constructors[0].setAccessible(true);
      InvocationTargetException thrown =
          assertThrows(
              InvocationTargetException.class,
              () -> constructors[0].newInstance(),
              "%s must also refuse reflective instantiation."
                  .formatted(holder.getSimpleName()));
      assertNotNull(thrown.getCause());
    }
  }

  /**
   * Skips synthetic methods, because JaCoCo adds a synthetic {@code $jacocoInit} to every loaded
   * class and an unfiltered count would fail under coverage.
   */
  private static List<String> authoredMethodsOf(Class<?> holder) {
    return Stream.of(holder.getDeclaredMethods())
        .filter(method -> !method.isSynthetic())
        .map(Method::getName)
        .sorted()
        .toList();
  }

  /**
   * The values of a holder's {@code public static final String} fields. It reads values rather than
   * field names, because a {@code DRAFT} field holding {@code "Draft"} would pass a field-name scan.
   */
  private static Set<String> constantsOf(Class<?> holder) {
    return new LinkedHashSet<>(declaredNamesOf(holder));
  }

  private static List<String> declaredNamesOf(Class<?> holder) {
    List<String> declared = new ArrayList<>();
    for (Field field : holder.getDeclaredFields()) {
      int modifiers = field.getModifiers();
      if (field.isSynthetic()
          || !Modifier.isPublic(modifiers)
          || !Modifier.isStatic(modifiers)
          || !Modifier.isFinal(modifiers)) {
        continue;
      }
      assertEquals(
          String.class,
          field.getType(),
          "%s.%s must be a String: a State or an Event is a bare name everywhere outside the Lifecycle."
              .formatted(holder.getSimpleName(), field.getName()));
      try {
        declared.add((String) field.get(null));
      } catch (IllegalAccessException e) {
        throw new AssertionError(
            "Could not read %s.%s".formatted(holder.getSimpleName(), field.getName()), e);
      }
    }
    assertFalse(
        declared.isEmpty(),
        "%s declares no constants. Either the holder is empty or this scan found nothing, and both are failures."
            .formatted(holder.getSimpleName()));
    return declared;
  }

  /** The enum's {@code valueOf} throws for an unknown name, so the {@code catch} is the assertion. */
  private static void assertEveryNameResolves(
      Set<String> names, Function<String, ?> valueOf, String enumName) {
    for (String name : names) {
      try {
        valueOf.apply(name);
      } catch (IllegalArgumentException e) {
        throw new AssertionError(
            "\"%s\" does not name a constant of %s. A Well-known name the enum does not carry is a typo, and it compiles into a comparison that never matches."
                .formatted(name, enumName),
            e);
      }
    }
  }
}
