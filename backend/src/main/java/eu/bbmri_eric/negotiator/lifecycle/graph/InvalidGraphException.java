package eu.bbmri_eric.negotiator.lifecycle.graph;

/**
 * A Definition Version describes a graph that cannot exist, so no compiled graph was produced.
 *
 * <p>Two initial States, a Transition leaving a State the version never declared, two Transitions
 * competing for one {@code (State, Event)} pair, rows from two versions handed to one compile, a
 * Wiring row naming a type key nothing declares, configuration that will not read into the type its
 * strategy declared — every one of these is this exception, and there is deliberately no second
 * type for any subset of them.
 *
 * <p><b>One type, not two.</b> These rejections were once split between {@code
 * IllegalArgumentException} and {@code IllegalStateException}, and the split carried no
 * information: what separated them was which class the author happened to be writing in, not
 * anything a caller could act on differently. Naming them once buys two things. A caller that has
 * to turn a corrupt definition into an HTTP response can catch graph corruption <em>and nothing
 * else</em> — a corrupt Definition Version Pin is a 500 and a log line, never a 403, 422 or 409,
 * and a catch clause naming {@code IllegalStateException} would swallow far more than that. And a
 * test can assert a class instead of a message substring, which is what stops assertions from
 * pinning prose.
 *
 * <p>It extends {@link IllegalStateException} because that is what each of these statements is: a
 * broken invariant about data the caller did not choose. Nobody passes a malformed graph as an
 * argument; they ask a question of one that was already wrong when it was seeded or published.
 *
 * <p>What this is <em>not</em> is a refused move. A Guard saying no, an Event that leads nowhere
 * from here, a caller without the authority — those are ordinary answers about a well-formed graph
 * and are reported as outcomes, never thrown.
 */
public class InvalidGraphException extends IllegalStateException {

  public InvalidGraphException(String message) {
    super(message);
  }

  public InvalidGraphException(String message, Throwable cause) {
    super(message, cause);
  }
}
