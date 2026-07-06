package eu.bbmri_eric.negotiator.lifecycle.statemachine;

public record TransitionOutcome<C extends TransitionContext>(
    String fromState, String toState, String event, C context, boolean fired) {}
