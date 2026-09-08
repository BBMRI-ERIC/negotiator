package eu.bbmri_eric.negotiator.lifecycle.graph;

import lombok.NonNull;

/**
 * What an Action receives when it runs: the domain facts that permitted the move, plus the
 * Transition that committed.
 *
 * <p>{@code committed.toState()} is the Lifecycle's new State, and {@code
 * evaluation.subject().currentState()} is the State it left.
 */
public record ActionContext(
    @NonNull EvaluationContext evaluation, @NonNull CompiledTransition committed) {}
