package eu.bbmri_eric.negotiator.lifecycle;

/**
 * The Resource Event names that behaviour outside the Lifecycle depends on <em>existing</em>. There
 * is one. It is a constant rather than a literal so that every name behaviour depends on lives in a
 * holder.
 *
 * <p>{@code OVERRIDE} is not a Well-known State. Every Definition Version carries the Override
 * Event, because it is the name under which an admin's direct State change appears in history. A
 * custom family can lack a Well-known State, but not this.
 */
public final class WellKnownResourceEvents {

  /**
   * The Override Event: an admin sets a Resource's State directly and skips the Transitions and
   * their Guards. The Event records that change in history.
   */
  public static final String OVERRIDE = "OVERRIDE";

  private WellKnownResourceEvents() {
    throw new AssertionError("WellKnownResourceEvents holds constants and is not instantiable.");
  }
}
