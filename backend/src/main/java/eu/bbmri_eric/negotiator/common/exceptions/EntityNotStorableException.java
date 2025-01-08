package eu.bbmri_eric.negotiator.common.exceptions;

public class EntityNotStorableException extends RuntimeException {

  private static final String errorMessage =
      "There was an error persisting the entity. Please try again later.";

  public EntityNotStorableException() {
    super(errorMessage);
  }

  public EntityNotStorableException(String message) {
    super(message);
  }
}
