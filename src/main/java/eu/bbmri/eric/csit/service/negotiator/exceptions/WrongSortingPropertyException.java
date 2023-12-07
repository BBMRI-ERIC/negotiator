package eu.bbmri.eric.csit.service.negotiator.exceptions;

public class WrongSortingPropertyException extends RuntimeException {
  private static final String errorMessage =
      "Unable to sort by property '%s'. Use one of the following: %s.";

  public WrongSortingPropertyException(String wrongProperty, String validSortingProperties) {
    super(errorMessage.formatted(wrongProperty, validSortingProperties));
  }
}
