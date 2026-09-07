package eu.bbmri_eric.negotiator.lifecycle.graph;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The two properties of the type itself, as opposed to the sites that raise it. Both are load
 * bearing and neither is visible from any test that only asserts {@code
 * isInstanceOf(InvalidGraphException.class)}, which is every other test of a rejection.
 */
class InvalidGraphExceptionTest {

  /**
   * The supertype is the decision, not an implementation detail. It is what makes a corrupt
   * definition a 500 rather than a 400 — {@code NegotiatorExceptionHandler} already maps {@code
   * IllegalStateException} to Internal Server Error and {@code IllegalArgumentException} to Bad
   * Request — and it is what D8 means by "a broken-invariant statement about data the caller did
   * not choose". Re-parenting this to {@code RuntimeException} would compile, pass every other test
   * in the subsystem, and quietly turn corrupt Definition Versions back into the caller's fault.
   */
  @Test
  @DisplayName("it is an IllegalStateException, which is what makes a corrupt graph a 500")
  void isAnIllegalStateException() {
    assertThat(new InvalidGraphException("any")).isInstanceOf(IllegalStateException.class);
  }

  /**
   * The registries wrap Jackson's {@code JsonProcessingException} when a Wiring row's configuration
   * will not read into the declared type. Their message names the key, the type and the blob, but
   * which field and which character are only in the cause, and that is the half an operator needs.
   */
  @Test
  @DisplayName("the wrapping constructor keeps the cause, which is where the parse detail lives")
  void keepsTheCause() {
    Throwable cause = new RuntimeException("the parser's own complaint");

    assertThat(new InvalidGraphException("outer", cause)).hasCause(cause);
  }
}
