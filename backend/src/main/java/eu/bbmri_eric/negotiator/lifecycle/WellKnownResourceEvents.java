package eu.bbmri_eric.negotiator.lifecycle;

/**
 * The Resource Event names that behaviour outside the Lifecycle depends on <em>existing</em>. There
 * is exactly one, and this holder exists to keep it from being spelled by hand in the one place
 * that sends it.
 *
 * <p>A holder for a single name looks like ceremony, and the alternative - a bare {@code
 * "OVERRIDE"} literal at the call site - was the honest option. It is rejected because the eleven
 * slices that follow this one all ask the same question of every State and Event name they meet,
 * "is this a name behaviour depends on, or data?", and an answer that is sometimes a constant and
 * sometimes a literal makes that question unanswerable by looking.
 *
 * <p><b>{@code OVERRIDE} is a different kind of dependency from a Well-known State, and
 * deliberately carries no matching vocabulary.</b> A Well-known State is a bet on a family's
 * vocabulary - a custom family may simply have no {@code DRAFT}. ADR 0002 instead makes the
 * Override Event structural to the model: it is the name under which an admin's direct State change
 * appears in history, so every Definition Version is expected to carry it. Naming it in Java
 * references a modelled concept rather than gambling on a name, which is why there is no
 * "Well-known Event" term in the glossary to go with these three holders. If a second, genuinely
 * fragile Event dependency ever appears, that term earns its place then.
 */
public final class WellKnownResourceEvents {

  /**
   * The Override Event: an administrator setting a Resource's State directly, bypassing the
   * Transitions and their guards. It is not a shortcut around the Lifecycle so much as the way such
   * a change gets recorded as one.
   */
  public static final String OVERRIDE = "OVERRIDE";

  private WellKnownResourceEvents() {
    throw new AssertionError("WellKnownResourceEvents holds constants and is not instantiable.");
  }
}
