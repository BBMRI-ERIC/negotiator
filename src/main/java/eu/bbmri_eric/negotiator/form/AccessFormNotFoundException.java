package eu.bbmri_eric.negotiator.form;

import java.util.List;

public class AccessFormNotFoundException extends RuntimeException {

  private static final String errorMessage = "Access Form with id %s not found";

  public AccessFormNotFoundException(Long accessFormId) {
    super(errorMessage.formatted(accessFormId));
  }

  public AccessFormNotFoundException(List<Long> entityId) {
    super(errorMessage.formatted(entityId));
  }

  public AccessFormNotFoundException(String entityId) {
    super(errorMessage.formatted(entityId));
  }
}
