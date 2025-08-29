package eu.bbmri_eric.negotiator.template;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class TemplateOperationRequest {
  public enum Operation {
    RESET
  }

  @NotNull private Operation operation;
}
