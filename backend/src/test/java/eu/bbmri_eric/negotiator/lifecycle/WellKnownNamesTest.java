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
 * Pins the three Well-known name holders to exactly nine names, and pins those names to what the
 * Lifecycle actually calls its States and Events today.
 *
 * <p>The holders carry the names that survive the deletion of the four Lifecycle enums, because
 * some behaviour depends on the name <em>existing</em> - a visibility rule that excludes drafts, a
 * trigger that spawns Resource Lifecycles, an admin override that bypasses guards. Everything else
 * in this slab carries a State or an Event name as data, off the column, the request or the
 * Definition Version Pin.
 *
 * <p><b>Rule one: the sets are exact, and that is the point of this test.</b> A holder that grows
 * toward declaring every State is the enum again with worse ergonomics - it restates in Java the
 * closed universe ADR 0002 exists to delete, and it would have to be kept in sync with a seed it
 * cannot see. The pressure to add "just one more" name is what this test exists to refuse, so it
 * asserts set equality rather than containment. Adding a name here is meant to require deleting a
 * line of this test, in a diff a reviewer will notice.
 *
 * <p><b>Rule two: every name is spelled the way the enum spells it.</b> These constants replace
 * enum references one subsystem at a time over the following eleven slices, and a typo in one would
 * not fail to compile - it would compile into a comparison that silently stops matching, which is a
 * behaviour change of exactly the kind this slab promises not to make. Pinning against the enums is
 * only possible while Spring Statemachine still runs, which is precisely why it is worth doing now.
 * At cutover the enums go and these three assertions go with them, as a compile error rather than
 * as a silent gap.
 *
 * <p><b>Rule three: a holder holds constants and nothing else.</b> No behaviour, not instantiable.
 * A holder that acquires a {@code labelFor} or a {@code values()} has become the lookup table the
 * Enum-Backed Lifecycle Catalog owns, and the catalog is deliberately somewhere else - inside the
 * state machine package, so the cutover deletes it with the library.
 */
class WellKnownNamesTest {

  /**
   * The five Negotiation States some behaviour depends on existing. ADR 0004 keeps a single
   * Negotiation-scope definition, so these are as stable as the enum was.
   */
  private static final Set<String> EXPECTED_NEGOTIATION_STATES =
      new LinkedHashSet<>(List.of("DRAFT", "SUBMITTED", "IN_PROGRESS", "DECLINED", "ABANDONED"));

  /**
   * The three Resource States the spawn loop writes. Unlike the Negotiation set, these are a bet on
   * a family's vocabulary: Resource scope is exactly the scope that diverges once custom Definition
   * Families ship. Recorded as a hazard rather than solved here.
   */
  private static final Set<String> EXPECTED_RESOURCE_STATES =
      new LinkedHashSet<>(
          List.of("SUBMITTED", "REPRESENTATIVE_CONTACTED", "REPRESENTATIVE_UNREACHABLE"));

  /**
   * The Override Event, and only it. ADR 0002 makes the Override Event structural rather than
   * conventional - it is how an admin's direct state change appears in history - so every
   * Definition Version is expected to carry it.
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
  @DisplayName("a holder holds constants only - no behaviour, and not instantiable")
  void everyHolder_isAHolderOfConstantsOnly() {
    for (Class<?> holder :
        List.of(
            WellKnownNegotiationStates.class,
            WellKnownResourceStates.class,
            WellKnownResourceEvents.class)) {
      assertTrue(
          Modifier.isFinal(holder.getModifiers()),
          "%s must be final: a subclass would be a place to add a tenth name out of sight."
              .formatted(holder.getSimpleName()));
      assertEquals(
          List.of(),
          authoredMethodsOf(holder),
          "%s must declare no methods. Lookup by name - label, description, ordinal - belongs to the Enum-Backed Lifecycle Catalog, which lives inside the state machine package so the cutover deletes it."
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
              "%s must refuse reflective instantiation too, or the private constructor is a hint rather than a rule."
                  .formatted(holder.getSimpleName()));
      assertNotNull(thrown.getCause());
    }
  }

  /**
   * The methods a holder's author wrote, which under a coverage agent is not the same thing as the
   * methods it has: JaCoCo instruments every loaded class with a synthetic {@code $jacocoInit}, so
   * an unfiltered method count fails here and passes in a plain compile. Synthetic members are the
   * compiler's and the agent's, never the author's, so excluding them is what makes this rule mean
   * what it says.
   */
  private static List<String> authoredMethodsOf(Class<?> holder) {
    return Stream.of(holder.getDeclaredMethods())
        .filter(method -> !method.isSynthetic())
        .map(Method::getName)
        .sorted()
        .toList();
  }

  /**
   * The names a holder declares, as the values of its {@code public static final String} fields.
   * Reading values rather than field names is deliberate: the two are expected to be identical, and
   * a holder whose {@code DRAFT} field held {@code "Draft"} would pass a field-name scan while
   * being exactly the bug rule two exists to catch.
   */
  private static Set<String> constantsOf(Class<?> holder) {
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

    Set<String> names = new LinkedHashSet<>(declared);
    assertEquals(
        declared.size(),
        names.size(),
        "%s declares two constants with the same value. Deduplicating them here would let a tenth field pass the exactness rule as an alias, which is the growth this test exists to refuse."
            .formatted(holder.getSimpleName()));
    return names;
  }

  private static void assertEveryNameResolves(
      Set<String> names, Function<String, ?> valueOf, String enumName) {
    for (String name : names) {
      try {
        assertNotNull(valueOf.apply(name));
      } catch (IllegalArgumentException e) {
        throw new AssertionError(
            "\"%s\" does not name a constant of %s. While Spring Statemachine still runs, a Well-known name the enum does not carry is a typo, and it would compile into a comparison that silently stops matching."
                .formatted(name, enumName),
            e);
      }
    }
  }
}
