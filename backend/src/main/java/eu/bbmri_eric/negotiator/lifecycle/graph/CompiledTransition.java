package eu.bbmri_eric.negotiator.lifecycle.graph;

import java.util.List;
import java.util.Objects;

/**
 * One edge of a compiled graph: firing {@code event} while in {@code fromState} moves to {@code
 * toState}, if the caller holds {@code requiredAuthority} and the Guard chain permits it.
 *
 * <p>States and Events are identified by name, because callers hold names from a {@code
 * current_state} column or a REST path. Row ids stay in the definition package.
 *
 * @param guards the <b>effective</b> Guard chain, in the order it runs. Definition-level Wiring
 *     entries have already been folded in ahead of this Transition's own, each group in its
 *     configured {@code sort_order}. ADR 0002 calls the effective chain "one query" for admin
 *     tooling; here it is one list, so the evaluator runs a single loop and never learns that
 *     Guards have two scopes. A chain assembled at fire time would have to know.
 */
public record CompiledTransition(
    String fromState,
    String event,
    String toState,
    RequiredAuthority requiredAuthority,
    List<GuardStep> guards) {

  public CompiledTransition {
    Objects.requireNonNull(fromState, "fromState");
    Objects.requireNonNull(event, "event");
    Objects.requireNonNull(toState, "toState");
    Objects.requireNonNull(
        requiredAuthority, "requiredAuthority: RequiredAuthority.NONE is how 'anyone' is spelled");
    guards = List.copyOf(guards);
  }

  /** A Transition with no Guards and no Actions. */
  public CompiledTransition(
      String fromState, String event, String toState, RequiredAuthority requiredAuthority) {
    this(fromState, event, toState, requiredAuthority, List.of());
  }
}
