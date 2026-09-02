package eu.bbmri_eric.negotiator.lifecycle.graph;

import java.util.List;
import java.util.Objects;

/**
 * One edge of a compiled graph: firing {@code event} while in {@code fromState} moves to {@code
 * toState}, if the caller holds {@code requiredAuthority} and the Guard chain permits it.
 *
 * <p>States and Events are named by their <em>names</em> rather than by their row ids. A compiled
 * graph is asked questions by callers holding a state string off a {@code current_state} column and
 * an event string off a REST path, and it is the only thing that knows a Definition Version at all
 * — so resolving those strings to ids only to resolve them back is work with no reader. The row ids
 * stay in the definition package, where the foreign keys that need them live.
 *
 * @param guards the <b>effective</b> Guard chain, in the order it runs. Definition-level Wiring
 *     entries have already been folded in ahead of this Transition's own, each group in its
 *     configured {@code sort_order}. ADR 0002 calls the effective chain "one query" for admin
 *     tooling; here it is one list, so the evaluator runs a single loop and never learns that
 *     Guards have two scopes. A chain assembled at fire time would have to know.
 * @param actions the Action chain, in {@code sort_order}. Transition-scoped only — there is no
 *     definition-wide Action wiring, and {@code action_wiring} has no definition column to express
 *     one with. Reported by a permitted outcome and run by the service that commits the move.
 */
public record CompiledTransition(
    String fromState,
    String event,
    String toState,
    RequiredAuthority requiredAuthority,
    List<GuardStep> guards,
    List<ActionStep> actions) {

  public CompiledTransition {
    Objects.requireNonNull(fromState, "fromState");
    Objects.requireNonNull(event, "event");
    Objects.requireNonNull(toState, "toState");
    Objects.requireNonNull(
        requiredAuthority, "requiredAuthority: RequiredAuthority.NONE is how 'anyone' is spelled");
    guards = List.copyOf(guards);
    actions = List.copyOf(actions);
  }

  /** An edge nothing guards, which is what all 21 Transitions of both v1 graphs are today. */
  public CompiledTransition(
      String fromState, String event, String toState, RequiredAuthority requiredAuthority) {
    this(fromState, event, toState, requiredAuthority, List.of(), List.of());
  }

  /** An edge with Guards and no Actions, which is 18 of the 21 today. */
  public CompiledTransition(
      String fromState,
      String event,
      String toState,
      RequiredAuthority requiredAuthority,
      List<GuardStep> guards) {
    this(fromState, event, toState, requiredAuthority, guards, List.of());
  }
}
