package eu.bbmri_eric.negotiator.lifecycle.graph;

/**
 * No Compiled Graph could be produced because the requested graph cannot exist: two initial
 * States, a Transition from an undeclared State, two Transitions for one {@code (State, Event)}
 * pair, rows from two versions in one compile, an unknown Wiring type key, or params that do not
 * read into the strategy's type. The catalogues also throw it when two strategies claim one type
 * key.
 *
 * <p>One type lets a caller catch graph corruption and nothing else, and lets a test assert a class
 * instead of a message. It extends {@link IllegalStateException} because the data was already
 * broken when it was seeded or published.
 *
 * <p>A refused move is not this exception. The Transition Evaluator reports refusals as outcomes.
 */
public class InvalidGraphException extends IllegalStateException {

  public InvalidGraphException(String message) {
    super(message);
  }

  public InvalidGraphException(String message, Throwable cause) {
    super(message, cause);
  }
}
