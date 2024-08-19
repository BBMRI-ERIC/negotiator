package eu.bbmri_eric.negotiator.common.exceptions;

public class UserNotFoundException extends RuntimeException {

  private static final String errorMessage = "User with id %s not found.";

  public UserNotFoundException(Long userId) {
    super(errorMessage.formatted(userId));
  }
}
