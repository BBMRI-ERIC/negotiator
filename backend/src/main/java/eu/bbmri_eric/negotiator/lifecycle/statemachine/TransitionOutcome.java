package eu.bbmri_eric.negotiator.lifecycle.statemachine;

public record TransitionOutcome<C extends TransitionContext>(
    String sourceState, String targetState, String event, C context) {}
