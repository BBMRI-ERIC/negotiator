package eu.bbmri_eric.negotiator.negotiation.dto;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Every name in the annotated collection is the name of a Negotiation State.
 *
 * <p>A list of bare names binds without complaint, so without this check an unknown status in a
 * filter would return an empty page instead of a 400, and a typo would look like a result.
 *
 * <p>A null collection and a null element are both valid. Only a name that names no State fails.
 */
@Documented
@Constraint(validatedBy = KnownNegotiationStateNamesValidator.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface KnownNegotiationStateNames {

  String message() default "must name States of the Negotiation Lifecycle";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
