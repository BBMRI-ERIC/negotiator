package eu.bbmri_eric.negotiator.lifecycle.statemachine;

import com.github.oxo42.stateless4j.StateMachine;
import java.util.List;

public final class NegotiatorStateMachine<C extends TransitionContext> {

  private final StateMachine<String, String> delegate;
  private final C context;
  private final List<TransitionListener<C>> listeners;

  NegotiatorStateMachine(StateMachine<String, String> delegate, C context, List<TransitionListener<C>> listeners) {
    this.delegate = delegate;
    this.context = context;
    this.listeners = listeners;
  }

  public String currentState() {
    return delegate.getState();
  }

  public TransitionOutcome<C> fire(String event) {
    String fromState = delegate.getState();
    if (!delegate.canFire(event)) {
      return new TransitionOutcome<>(fromState, fromState, event, context, false);
    }
    delegate.fire(event);
    TransitionOutcome<C> outcome = new TransitionOutcome<>(fromState, delegate.getState(), event, context, true);
    listeners.forEach(listener -> listener.onTransition(outcome));
    return outcome;
  }
}
