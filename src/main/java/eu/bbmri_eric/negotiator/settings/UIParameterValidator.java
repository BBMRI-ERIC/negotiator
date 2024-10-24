package eu.bbmri_eric.negotiator.settings;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class UIParameterValidator implements ConstraintValidator<ValidUIParameter, UIParameter> {

  @Override
  public void initialize(ValidUIParameter constraintAnnotation) {}

  @Override
  public boolean isValid(UIParameter parameter, ConstraintValidatorContext context) {
    switch (parameter.getType()) {
      case BOOL:
        return parameter.getValue().equalsIgnoreCase("true")
            || parameter.getValue().equalsIgnoreCase("false");
      case INT:
        try {
          Integer.parseInt(parameter.getValue());
          return true;
        } catch (NumberFormatException e) {
          return false;
        }
      case STRING:
        return true;
      default:
        return false;
    }
  }
}
