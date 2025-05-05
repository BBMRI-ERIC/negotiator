package eu.bbmri_eric.negotiator.notification.email;

import jakarta.validation.constraints.NotNull;

public class EmailTemplateOperationRequest {
  public enum Operation {
    RESET
  }

  @NotNull private Operation operation;

  public Operation getOperation() {
    return operation;
  }

  public void setOperation(Operation operation) {
    this.operation = operation;
  }
}
