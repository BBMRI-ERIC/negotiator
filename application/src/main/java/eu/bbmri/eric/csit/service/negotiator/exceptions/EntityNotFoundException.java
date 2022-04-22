package eu.bbmri.eric.csit.service.negotiator.exceptions;

public class EntityNotFoundException extends RuntimeException {

  private static final String errorMessage = "Resource with id %s not found";

  public EntityNotFoundException(Long entityId) {
    super(errorMessage.formatted(entityId));
  }
}
