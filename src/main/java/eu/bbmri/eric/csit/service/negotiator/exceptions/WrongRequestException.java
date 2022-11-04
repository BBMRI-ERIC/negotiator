package eu.bbmri.eric.csit.service.negotiator.exceptions;

public class WrongRequestException extends RuntimeException {

  private static final String errorMessage = "The request has some errors";

  public WrongRequestException() {
    super(errorMessage);
  }

  public WrongRequestException(String message) {
    super(message);
  }
}
