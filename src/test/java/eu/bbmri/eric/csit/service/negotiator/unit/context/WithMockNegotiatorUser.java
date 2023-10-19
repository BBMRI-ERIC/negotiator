package eu.bbmri.eric.csit.service.negotiator.unit.context;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import org.springframework.security.test.context.support.WithSecurityContext;

/**
 * Annotation to be used in unit tests similar to standard @WithMockUser but tailored on Negotiator User.
 * It includes properties of a Person necessary in the authn/authz process
 */
@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithMockCustomUserSecurityContextFactory.class)
public @interface WithMockNegotiatorUser {

    long id() default 1L;

    String authSubject() default "user@negotiator";

    String authName() default "USER";

    String authEmail() default "user@negotiator";

    String[] authorities() default {};

    String[] roles() default {};
}
