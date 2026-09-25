package eu.bbmri_eric.negotiator.negotiation.dto;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = PayloadKeysValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPayloadKeys {

  String message() default "Payload field names must not contain '<', '>' or '&'";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
