package eu.bbmri_eric.negotiator.webhook;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Metadata used for generated OpenAPI webhook header documentation. */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface WebhookHeaderDoc {
  /** Human-readable webhook header description shown in OpenAPI docs. */
  String description();

  /** Whether the webhook header is required on delivery. */
  boolean required();

  /** Optional OpenAPI example value for the header. */
  String example() default "";
}
