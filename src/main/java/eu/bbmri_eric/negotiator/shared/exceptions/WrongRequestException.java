package eu.bbmri_eric.negotiator.shared.exceptions;

public class WrongRequestException extends RuntimeException {

  private static final String errorMessage = "The negotiation has some errors";

  public WrongRequestException() {
    super(errorMessage);
  }

  public WrongRequestException(String message) {
    super(message);
  }
}
