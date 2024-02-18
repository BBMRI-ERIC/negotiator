package eu.bbmri_eric.negotiator.exceptions;

import java.util.List;

public class EntityNotFoundException extends RuntimeException {

  private static final String errorMessage = "Resource with id %s not found";

  public EntityNotFoundException(Long entityId) {
    super(errorMessage.formatted(entityId));
  }

  public EntityNotFoundException(List<Long> entityId) {
    super(errorMessage.formatted(entityId));
  }

  public EntityNotFoundException(String entityId) {
    super(errorMessage.formatted(entityId));
  }
}
