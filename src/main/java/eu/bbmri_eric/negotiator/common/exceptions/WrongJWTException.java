package eu.bbmri_eric.negotiator.common.exceptions;

public class WrongJWTException extends RuntimeException {

  private static final String error_message =
      "The userinfo endpoint could not be reached and the provided JWT does not contain all necessary information.";

  public WrongJWTException() {
    super(error_message);
  }
}
