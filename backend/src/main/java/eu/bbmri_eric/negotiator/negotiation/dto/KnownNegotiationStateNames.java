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
 * <p>This exists to keep one answer the API has always given. While the status filter was a list of
 * enum constants, Spring refused an unknown name while binding the request and the client got a
 * 400. A list of bare names binds happily, so without this the same request would be answered with
 * an empty page - "no such Negotiations" where the client asked "which Negotiations have this
 * status", and a typo would be reported as a result rather than as a mistake.
 *
 * <p>A null collection and a null element are both valid; only a name that names no State is not.
 */
@Documented
@Constraint(validatedBy = KnownNegotiationStateNamesValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface KnownNegotiationStateNames {

  String message() default "must name States of the Negotiation Lifecycle";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
