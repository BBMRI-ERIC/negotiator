package eu.bbmri.eric.csit.service.negotiator.exceptions;

public class WrongJWTException extends RuntimeException {

  private static final String error_message =
      "The userinfo endpoint could not be reached and the provided JWT does not contain all necessary information.";

  public WrongJWTException() {
    super(error_message);
  }
}
