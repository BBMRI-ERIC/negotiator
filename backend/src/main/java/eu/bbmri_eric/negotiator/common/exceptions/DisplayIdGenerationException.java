package eu.bbmri_eric.negotiator.common.exceptions;

/** Exception thrown when the display ID generation process fails. */
public class DisplayIdGenerationException extends RuntimeException {

  private static final String error_message =
      "The display ID generation process failed. This could be due to an issue with the display ID "
          + "configuration.";

  public DisplayIdGenerationException() {
    super(error_message);
  }

  public DisplayIdGenerationException(String message) {
    super(message);
  }
}
