package eu.bbmri.eric.csit.service.negotiator.exceptions;

public class EntityNotFoundException extends RuntimeException {

  private static final String errorMessage = "%s with id %s not found";

  public EntityNotFoundException(Class<?> klass, Long entityId) {
    super(errorMessage.formatted(klass.getName(), entityId));
  }
}
