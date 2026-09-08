package eu.bbmri_eric.negotiator.lifecycle.graph;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Two properties of the exception type itself. Tests of individual rejections only assert {@code
 * isInstanceOf(InvalidGraphException.class)}, so they would not catch a change to either.
 */
class InvalidGraphExceptionTest {

  /**
   * {@code NegotiatorExceptionHandler} maps {@code IllegalStateException} to 500 and {@code
   * IllegalArgumentException} to 400, so this supertype keeps a corrupt Definition Version from
   * reading as the caller's fault. Changing it to {@code RuntimeException} would still compile and
   * pass every other test.
   */
  @Test
  @DisplayName("it is an IllegalStateException, which is what makes a corrupt graph a 500")
  void isAnIllegalStateException() {
    assertThat(new InvalidGraphException("any")).isInstanceOf(IllegalStateException.class);
  }

  /**
   * The registries wrap Jackson's {@code JsonProcessingException} when a Wiring row's configuration
   * does not read into the declared type. Only the cause says which field and character failed.
   */
  @Test
  @DisplayName("the wrapping constructor keeps the cause, which is where the parse detail lives")
  void keepsTheCause() {
    Throwable cause = new RuntimeException("the parser's own complaint");

    assertThat(new InvalidGraphException("outer", cause)).hasCause(cause);
  }
}
