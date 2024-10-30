package eu.bbmri_eric.negotiator.settings;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = UIParameterValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidUIParameter {

  String message() default "Invalid value for type";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
