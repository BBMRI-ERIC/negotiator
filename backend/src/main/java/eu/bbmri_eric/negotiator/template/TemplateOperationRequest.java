package eu.bbmri_eric.negotiator.template;

import jakarta.validation.constraints.NotNull;

public class TemplateOperationRequest {
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
