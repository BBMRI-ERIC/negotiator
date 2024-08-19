package eu.bbmri_eric.negotiator.common.exceptions;

public class WrongSortingPropertyException extends RuntimeException {
  private static final String errorMessage =
      "Unable to sort by property '%s'. Use one of the following: %s.";

  public WrongSortingPropertyException(String wrongProperty, String validSortingProperties) {
    super(errorMessage.formatted(wrongProperty, validSortingProperties));
  }
}
