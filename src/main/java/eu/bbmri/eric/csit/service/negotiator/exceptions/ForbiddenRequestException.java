package eu.bbmri.eric.csit.service.negotiator.exceptions;

public class ForbiddenRequestException extends RuntimeException {

  private static final String errorMessage = "Cannot access this object";

  public ForbiddenRequestException() {
    super(errorMessage);
  }

  public ForbiddenRequestException(String message) {
    super(message);
  }
}
