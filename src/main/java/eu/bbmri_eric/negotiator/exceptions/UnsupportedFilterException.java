package eu.bbmri_eric.negotiator.exceptions;

import java.util.Arrays;

public class UnsupportedFilterException extends RuntimeException {

  private static final String error_message =
      "The property: '%s' is not a valid filter. Use one of the following values: %s .";

  public UnsupportedFilterException(String property, String[] supportedProperties) {
    super(error_message.formatted(property, Arrays.toString(supportedProperties)));
  }
}
