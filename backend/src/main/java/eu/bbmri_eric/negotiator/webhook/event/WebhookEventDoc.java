package eu.bbmri_eric.negotiator.webhook.event;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Metadata used for generated OpenAPI webhook documentation. */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface WebhookEventDoc {
  /** Human-readable webhook summary shown in OpenAPI operation docs. */
  String summary();

  /** Optional detailed webhook description shown in OpenAPI docs. */
  String description() default "";
}
